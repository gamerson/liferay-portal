package licensing

import (
	"context"
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/pem"
	"fmt"
	"strings"
	"time"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	apierrors "k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/api/meta"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	logf "sigs.k8s.io/controller-runtime/pkg/log"

	licensingv1alpha1 "github.com/liferay/liferay-portal/cloud/operator/api/licensing/v1alpha1"
	"github.com/liferay/liferay-portal/cloud/operator/internal/provisioning"
)

// LiferayEnvironmentReconciler drives the licensing lifecycle for a single
// LiferayEnvironment.
type LiferayEnvironmentReconciler struct {
	client.Client

	// Provisioning is the client to the provisioning/marketplace APIs. When
	// nil (early prototyping without a sandbox) network steps are skipped and
	// reported through the ProvisioningReachable condition.
	Provisioning provisioning.Client
}

const (
	conditionActivated             = "Activated"
	conditionLicenseValid          = "LicenseValid"
	conditionProvisioningReachable = "ProvisioningReachable"
	conditionReplicasClamped       = "ReplicasClamped"

	identitySecretSuffix = "-cne-identity"
	licenseSecretSuffix  = "-cne-license"
)

// +kubebuilder:rbac:groups=licensing.liferay.com,resources=liferayenvironments,verbs=get;list;watch;update;patch
// +kubebuilder:rbac:groups=licensing.liferay.com,resources=liferayenvironments/status,verbs=get;update;patch
// +kubebuilder:rbac:groups=licensing.liferay.com,resources=liferayenvironments/finalizers,verbs=update
// +kubebuilder:rbac:groups="",resources=secrets,verbs=get;list;watch;create;update;patch
// +kubebuilder:rbac:groups="",resources=namespaces,verbs=get;list;watch
// +kubebuilder:rbac:groups="",resources=events,verbs=create;patch
// +kubebuilder:rbac:groups=apps,resources=statefulsets,verbs=get;list;watch;patch
// +kubebuilder:rbac:groups=apps,resources=statefulsets/scale,verbs=get;update;patch
// +kubebuilder:rbac:groups=apps,resources=deployments,verbs=get;list;watch;patch
// +kubebuilder:rbac:groups=apps,resources=deployments/scale,verbs=get;update;patch

func (r *LiferayEnvironmentReconciler) Reconcile(
	ctx context.Context,
	req ctrl.Request,
) (ctrl.Result, error) {

	log := logf.FromContext(ctx)

	env := &licensingv1alpha1.LiferayEnvironment{}

	if err := r.Get(ctx, req.NamespacedName, env); err != nil {
		return ctrl.Result{}, client.IgnoreNotFound(err)
	}

	// environmentId is the namespace UID: stable, unique, DDOS-allowlisted.

	environmentId, err := r.resolveEnvironmentId(ctx, env.Namespace)

	if err != nil {
		return ctrl.Result{}, err
	}

	env.Status.EnvironmentId = environmentId

	// Ensure the cluster keypair exists; the private key never leaves here.

	privateKey, err := r.ensureIdentity(ctx, env)

	if err != nil {
		return ctrl.Result{}, err
	}

	// Activation (one-time). Skipped once status records success.

	if env.Status.ActivatedAt == nil {
		if r.Provisioning == nil {
			r.markProvisioningSkipped(env)

			return r.finish(ctx, env)
		}

		publicKey, err := publicKeyPEM(privateKey)

		if err != nil {
			return ctrl.Result{}, err
		}

		activationCode, err := r.readActivationCode(ctx, env)

		if err != nil {
			return ctrl.Result{}, err
		}

		activateErr := r.Provisioning.Activate(
			ctx,
			privateKey,
			provisioning.ActivationRequest{
				ActivationCode:  activationCode,
				EnvironmentId:   environmentId,
				EnvironmentName: env.Spec.EnvironmentName,
				PublicKey:       publicKey,
			},
		)

		if activateErr != nil {
			meta.SetStatusCondition(
				&env.Status.Conditions,
				metav1.Condition{
					Type:    conditionActivated,
					Status:  metav1.ConditionFalse,
					Reason:  "ActivationRejected",
					Message: activateErr.Error(),
				},
			)

			env.Status.Phase = "Degraded"

			return r.finish(ctx, env)
		}

		now := metav1.Now()
		env.Status.ActivatedAt = &now

		meta.SetStatusCondition(
			&env.Status.Conditions,
			metav1.Condition{
				Type:   conditionActivated,
				Status: metav1.ConditionTrue,
				Reason: "Activated",
			},
		)
	}

	// Entitlements heartbeat.

	if r.Provisioning == nil {
		r.markProvisioningSkipped(env)

		return r.finish(ctx, env)
	}

	entitlements, err := r.Provisioning.Entitlements(
		ctx,
		privateKey,
		provisioning.EntitlementsRequest{
			DxpVersion:    r.resolveDxpVersion(env),
			EnvironmentId: environmentId,
		},
	)

	if err != nil {
		// Keep last-known-good; do not yank a valid license on a blip.

		meta.SetStatusCondition(
			&env.Status.Conditions,
			metav1.Condition{
				Type:    conditionProvisioningReachable,
				Status:  metav1.ConditionFalse,
				Reason:  "EntitlementsFetchFailed",
				Message: err.Error(),
			},
		)

		env.Status.Phase = "Degraded"

		return r.finish(ctx, env)
	}

	meta.SetStatusCondition(
		&env.Status.Conditions,
		metav1.Condition{
			Type:   conditionProvisioningReachable,
			Status: metav1.ConditionTrue,
			Reason: "Reachable",
		},
	)

	env.Status.License.MaxClusterNodes = entitlements.MaxClusterNodes

	// TODO: persist entitlements.LicenseXML into the license Secret so the
	// sync sidecar can copy it into each pod's deploy/ directory.

	// Clamp the workload replicas to the licensed node ceiling.

	if err := r.clampReplicas(ctx, env); err != nil {
		return ctrl.Result{}, err
	}

	// Download each entitled add-on. TODO: write the lpkg to the artifact PVC
	// and track it by checksum so the sync sidecar can deploy it; for now the
	// download proves the marketplace path end to end.
	for _, app := range entitlements.Apps {
		virtualEntryId := parseVirtualEntryId(app.LpkgDownloadLink)

		lpkg, downloadErr := r.Provisioning.DownloadApp(
			ctx,
			privateKey,
			app.LpkgDownloadLink,
			provisioning.DownloadRequest{
				EnvironmentId:  environmentId,
				VirtualEntryId: virtualEntryId,
			},
		)

		if downloadErr != nil {
			log.Error(
				downloadErr, "Add-on download failed",
				"name", app.Name, "virtualEntryId", virtualEntryId,
			)

			continue
		}

		log.Info(
			"Downloaded add-on",
			"name", app.Name,
			"virtualEntryId", virtualEntryId,
			"bytes", len(lpkg),
		)
	}

	env.Status.Phase = "Ready"

	return r.finish(ctx, env)
}

// finish persists status and requeues on the heartbeat interval.
func (r *LiferayEnvironmentReconciler) finish(
	ctx context.Context,
	env *licensingv1alpha1.LiferayEnvironment,
) (ctrl.Result, error) {

	if err := r.Status().Update(ctx, env); err != nil {
		if apierrors.IsConflict(err) {
			// The object changed under us (a concurrent edit, or an event
			// from a prior reconcile's own write). This is benign: requeue
			// promptly and re-reconcile against the fresh object rather than
			// surfacing it as an error.
			return ctrl.Result{RequeueAfter: time.Second}, nil
		}

		return ctrl.Result{}, err
	}

	return ctrl.Result{RequeueAfter: env.Spec.HeartbeatInterval.Duration}, nil
}

func (r *LiferayEnvironmentReconciler) markProvisioningSkipped(
	env *licensingv1alpha1.LiferayEnvironment,
) {

	meta.SetStatusCondition(
		&env.Status.Conditions,
		metav1.Condition{
			Type:    conditionProvisioningReachable,
			Status:  metav1.ConditionFalse,
			Reason:  "NoProvisioningClient",
			Message: "Provisioning client not configured (prototype).",
		},
	)

	env.Status.Phase = "Pending"
}

func (r *LiferayEnvironmentReconciler) resolveEnvironmentId(
	ctx context.Context,
	namespace string,
) (string, error) {

	ns := &corev1.Namespace{}

	if err := r.Get(ctx, types.NamespacedName{Name: namespace}, ns); err != nil {
		return "", err
	}

	return string(ns.UID), nil
}

func (r *LiferayEnvironmentReconciler) resolveDxpVersion(
	env *licensingv1alpha1.LiferayEnvironment,
) string {

	if env.Spec.DxpVersion != "" {
		return env.Spec.DxpVersion
	}

	// TODO: derive from the workload's container image tag.

	return ""
}

// ensureIdentity returns the environment's cluster private key, generating and
// storing the keypair if the identity Secret does not yet exist. The private
// key never leaves the agent; it signs every JWT to provisioning.
func (r *LiferayEnvironmentReconciler) ensureIdentity(
	ctx context.Context,
	env *licensingv1alpha1.LiferayEnvironment,
) (*rsa.PrivateKey, error) {

	name := env.Name + identitySecretSuffix

	secret := &corev1.Secret{}
	key := types.NamespacedName{Namespace: env.Namespace, Name: name}

	err := r.Get(ctx, key, secret)

	if err == nil {
		return parsePrivateKey(secret.Data["private.pem"])
	}

	if !apierrors.IsNotFound(err) {
		return nil, err
	}

	privateKey, err := rsa.GenerateKey(rand.Reader, 2048)

	if err != nil {
		return nil, err
	}

	privateBytes, err := x509.MarshalPKCS8PrivateKey(privateKey)

	if err != nil {
		return nil, err
	}

	privatePEM := pem.EncodeToMemory(
		&pem.Block{Type: "PRIVATE KEY", Bytes: privateBytes},
	)

	publicPEM, err := publicKeyPEM(privateKey)

	if err != nil {
		return nil, err
	}

	secret = &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: env.Namespace,
			Name:      name,
			Labels:    map[string]string{"controller-watched": "yes"},
		},
		Data: map[string][]byte{
			"private.pem": privatePEM,
			"public.pem":  []byte(publicPEM),
		},
	}

	if err := ctrl.SetControllerReference(env, secret, r.Scheme()); err != nil {
		return nil, err
	}

	if err := r.Create(ctx, secret); err != nil {
		return nil, err
	}

	return privateKey, nil
}

// parsePrivateKey decodes a PKCS#8 PEM-encoded RSA private key.
func parsePrivateKey(data []byte) (*rsa.PrivateKey, error) {
	block, _ := pem.Decode(data)

	if block == nil {
		return nil, fmt.Errorf("identity secret: no PEM block in private.pem")
	}

	parsed, err := x509.ParsePKCS8PrivateKey(block.Bytes)

	if err != nil {
		return nil, err
	}

	privateKey, ok := parsed.(*rsa.PrivateKey)

	if !ok {
		return nil, fmt.Errorf("identity secret: not an RSA private key")
	}

	return privateKey, nil
}

// publicKeyPEM returns the PKIX PEM encoding of a private key's public half.
func publicKeyPEM(key *rsa.PrivateKey) (string, error) {
	publicBytes, err := x509.MarshalPKIXPublicKey(&key.PublicKey)

	if err != nil {
		return "", err
	}

	return string(
		pem.EncodeToMemory(
			&pem.Block{Type: "PUBLIC KEY", Bytes: publicBytes},
		),
	), nil
}

// readActivationCode loads the one-time activation code from the referenced
// Secret in the environment's namespace.
func (r *LiferayEnvironmentReconciler) readActivationCode(
	ctx context.Context,
	env *licensingv1alpha1.LiferayEnvironment,
) (string, error) {

	ref := env.Spec.ActivationCodeSecretRef

	secret := &corev1.Secret{}
	key := types.NamespacedName{Namespace: env.Namespace, Name: ref.Name}

	if err := r.Get(ctx, key, secret); err != nil {
		return "", err
	}

	code, ok := secret.Data[ref.Key]

	if !ok {
		return "", fmt.Errorf(
			"activation code secret %q missing key %q", ref.Name, ref.Key,
		)
	}

	return string(code), nil
}

// clampReplicas enforces effective = min(desired, maxClusterNodes) on the
// referenced workload and reports whether the desired count was clamped.
func (r *LiferayEnvironmentReconciler) clampReplicas(
	ctx context.Context,
	env *licensingv1alpha1.LiferayEnvironment,
) error {

	desired := env.Spec.DesiredReplicas
	max := env.Status.License.MaxClusterNodes

	effective := desired

	clamped := false

	if max > 0 && desired > max {
		effective = max
		clamped = true
	}

	env.Status.EffectiveReplicas = effective

	condition := metav1.ConditionFalse
	reason := "WithinLimit"

	if clamped {
		condition = metav1.ConditionTrue
		reason = "ExceedsLicensedNodes"
	}

	meta.SetStatusCondition(
		&env.Status.Conditions,
		metav1.Condition{
			Type:   conditionReplicasClamped,
			Status: condition,
			Reason: reason,
			Message: fmt.Sprintf(
				"desired=%d licensed max=%d effective=%d",
				desired, max, effective,
			),
		},
	)

	if env.Spec.WorkloadRef.Kind != "StatefulSet" {
		// TODO: Deployment support.

		return nil
	}

	statefulSet := &appsv1.StatefulSet{}
	key := types.NamespacedName{
		Namespace: env.Namespace,
		Name:      env.Spec.WorkloadRef.Name,
	}

	if err := r.Get(ctx, key, statefulSet); err != nil {
		return client.IgnoreNotFound(err)
	}

	if statefulSet.Spec.Replicas != nil &&
		*statefulSet.Spec.Replicas == effective {

		return nil
	}

	patch := client.MergeFrom(statefulSet.DeepCopy())
	statefulSet.Spec.Replicas = &effective

	return r.Patch(ctx, statefulSet, patch)
}

// parseVirtualEntryId extracts the artifact ID embedded in a marketplace
// download URL: .../marketplace/virtual-entry/{id}/download.
func parseVirtualEntryId(link string) string {
	const marker = "/virtual-entry/"

	i := strings.Index(link, marker)

	if i < 0 {
		return ""
	}

	rest := link[i+len(marker):]

	if j := strings.IndexByte(rest, '/'); j >= 0 {
		return rest[:j]
	}

	return rest
}

func (r *LiferayEnvironmentReconciler) SetupWithManager(mgr ctrl.Manager) error {
	return ctrl.NewControllerManagedBy(
		mgr,
	).For(
		&licensingv1alpha1.LiferayEnvironment{},
	).Owns(
		&corev1.Secret{},
	).Named(
		"liferayenvironment",
	).Complete(
		r,
	)
}
