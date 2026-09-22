package cx

import (
	"context"
	"fmt"
	"time"

	cxv1alpha1 "github.com/liferay/liferay-portal/cloud/operator/api/cx/v1alpha1"
	licensingv1alpha1 "github.com/liferay/liferay-portal/cloud/operator/api/licensing/v1alpha1"
	appsv1 "k8s.io/api/apps/v1"
	batchv1 "k8s.io/api/batch/v1"
	corev1 "k8s.io/api/core/v1"
	apierrors "k8s.io/apimachinery/pkg/api/errors"
	meta "k8s.io/apimachinery/pkg/api/meta"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	types "k8s.io/apimachinery/pkg/types"
	record "k8s.io/client-go/tools/record"
	controllerruntime "sigs.k8s.io/controller-runtime"
	client "sigs.k8s.io/controller-runtime/pkg/client"
	controllerutil "sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
	handler "sigs.k8s.io/controller-runtime/pkg/handler"
	reconcile "sigs.k8s.io/controller-runtime/pkg/reconcile"
	source "sigs.k8s.io/controller-runtime/pkg/source"
)

const fieldOwner = "dxp-operator-cx"

// ClientExtensionReconciler delivers a client extension's configuration to
// Liferay and owns the resulting workload.
type ClientExtensionReconciler struct {
	client.Client

	// ProvisioningGracePeriod bounds how long deletion waits for Liferay to
	// acknowledge unprovisioning before the finalizer is released. Deletion
	// cannot block forever, but releasing early can orphan OAuth2
	// applications, so the wait is logged when it expires.
	ProvisioningGracePeriod time.Duration

	// RequeueInterval is how often a client extension waiting on Liferay is
	// re-examined.
	RequeueInterval time.Duration

	Recorder record.EventRecorder
}

// +kubebuilder:rbac:groups=cx.liferay.com,resources=clientextensions,verbs=get;list;watch;create;update;patch;delete
// +kubebuilder:rbac:groups=cx.liferay.com,resources=clientextensions/status,verbs=get;update;patch
// +kubebuilder:rbac:groups=cx.liferay.com,resources=clientextensions/finalizers,verbs=update
// +kubebuilder:rbac:groups=licensing.liferay.com,resources=liferayenvironments,verbs=get;list;watch
// +kubebuilder:rbac:groups="",resources=configmaps;secrets,verbs=get;list;watch;create;update;patch;delete
// +kubebuilder:rbac:groups=apps,resources=deployments,verbs=get;list;watch;create;update;patch;delete
// +kubebuilder:rbac:groups=batch,resources=cronjobs;jobs,verbs=get;list;watch;create;update;patch;delete
// +kubebuilder:rbac:groups="",resources=events,verbs=create;patch

func (reconciler *ClientExtensionReconciler) Reconcile(
	context context.Context, request controllerruntime.Request,
) (controllerruntime.Result, error) {
	logger := controllerruntime.LoggerFrom(context)

	var clientExtension cxv1alpha1.ClientExtension

	if error := reconciler.Get(context, request.NamespacedName, &clientExtension); error != nil {
		return controllerruntime.Result{}, client.IgnoreNotFound(error)
	}

	if clientExtension.DeletionTimestamp != nil {
		return reconciler.finalize(context, &clientExtension)
	}

	if !controllerutil.ContainsFinalizer(&clientExtension, Finalizer) {
		controllerutil.AddFinalizer(&clientExtension, Finalizer)

		if error := reconciler.Update(context, &clientExtension); error != nil {
			return controllerruntime.Result{}, error
		}
	}

	liferayNamespace, error := reconciler.resolveEnvironment(context, &clientExtension)

	if error != nil {
		return reconciler.degrade(context, &clientExtension, "EnvironmentUnusable", error.Error())
	}

	dxpMetadataName := DXPMetadataName(clientExtension.Spec.VirtualInstanceID)

	var dxpMetadata corev1.ConfigMap

	if error := reconciler.Get(
		context,
		types.NamespacedName{Name: dxpMetadataName, Namespace: liferayNamespace},
		&dxpMetadata,
	); error != nil {
		if !apierrors.IsNotFound(error) {
			return controllerruntime.Result{}, error
		}

		return reconciler.degrade(
			context, &clientExtension, "UnknownVirtualInstance",
			fmt.Sprintf(
				"ConfigMap %q does not exist in namespace %q; the virtual instance is unknown to Liferay.",
				dxpMetadataName, liferayNamespace,
			),
		)
	}

	if error := reconciler.applyExtProvision(
		context, &clientExtension, liferayNamespace,
	); error != nil {
		return controllerruntime.Result{}, error
	}

	setCondition(
		&clientExtension, cxv1alpha1.ConditionDelivered, metav1.ConditionTrue, "Applied",
		fmt.Sprintf("Configuration payload applied into namespace %q.", liferayNamespace),
	)

	extInit, error := reconciler.findExtInit(context, &clientExtension, liferayNamespace)

	if error != nil {
		return controllerruntime.Result{}, error
	}

	if extInit == nil {
		setCondition(
			&clientExtension, cxv1alpha1.ConditionProvisioned, metav1.ConditionFalse, "AwaitingLiferay",
			"Waiting for Liferay to write back the ext-init metadata.",
		)
		setCondition(
			&clientExtension, cxv1alpha1.ConditionReady, metav1.ConditionFalse, "AwaitingProvisioning",
			"The workload is not created until the OAuth2 credentials exist.",
		)

		clientExtension.Status.Phase = cxv1alpha1.PhasePending

		return reconciler.requeue(context, &clientExtension)
	}

	extInitSecretName, error := reconciler.mirrorExtInit(context, &clientExtension, extInit)

	if error != nil {
		return controllerruntime.Result{}, error
	}

	setCondition(
		&clientExtension, cxv1alpha1.ConditionProvisioned, metav1.ConditionTrue, "Provisioned",
		fmt.Sprintf("Liferay published %q; credentials mirrored into %q.", extInit.Name, extInitSecretName),
	)

	clientExtension.Status.ExtInitSecretName = extInitSecretName

	dxpMetadataSource := dxpMetadataName

	if liferayNamespace != clientExtension.Namespace {
		mirrored, error := reconciler.ensureDXPMetadataMirror(
			context, &clientExtension, &dxpMetadata,
		)

		if error != nil {
			return reconciler.degrade(context, &clientExtension, "MirrorSourceConflict", error.Error())
		}

		dxpMetadataSource = mirrored
	}

	ready, error := reconciler.applyWorkload(context, &clientExtension, WorkloadSources{
		DXPMetadataConfigMapName: dxpMetadataSource,
		ExtInitSecretName:        extInitSecretName,
	})

	if error != nil {
		return controllerruntime.Result{}, error
	}

	if ready {
		setCondition(
			&clientExtension, cxv1alpha1.ConditionReady, metav1.ConditionTrue, "Available",
			"The client extension is provisioned and its workload is available.",
		)

		clientExtension.Status.Phase = cxv1alpha1.PhaseReady
	} else {
		setCondition(
			&clientExtension, cxv1alpha1.ConditionReady, metav1.ConditionFalse, "WorkloadNotAvailable",
			"The workload has been created and is not yet available.",
		)

		clientExtension.Status.Phase = cxv1alpha1.PhasePending
	}

	logger.V(1).Info(
		"Reconciled client extension",
		"liferayNamespace", liferayNamespace, "phase", clientExtension.Status.Phase,
	)

	return reconciler.requeue(context, &clientExtension)
}

func (reconciler *ClientExtensionReconciler) SetupWithManager(
	manager controllerruntime.Manager,
) error {
	mapConfigMap := func(
		context context.Context, object client.Object,
	) []reconcile.Request {
		labels := object.GetLabels()

		if labels[LabelMetadataType] != MetadataTypeExtInit &&
			labels[LabelMetadataType] != MetadataTypeDXP {

			return nil
		}

		var list cxv1alpha1.ClientExtensionList

		if error := reconciler.List(context, &list); error != nil {
			return nil
		}

		var requests []reconcile.Request

		for index := range list.Items {
			clientExtension := &list.Items[index]

			if LiferayNamespace(clientExtension) != object.GetNamespace() {
				continue
			}

			if clientExtension.Spec.VirtualInstanceID != labels[LabelVirtualInstance] {
				continue
			}

			requests = append(requests, reconcile.Request{
				NamespacedName: types.NamespacedName{
					Name: clientExtension.Name, Namespace: clientExtension.Namespace,
				},
			})
		}

		return requests
	}

	return controllerruntime.NewControllerManagedBy(manager).
		For(&cxv1alpha1.ClientExtension{}).
		Owns(&appsv1.Deployment{}).
		Owns(&batchv1.CronJob{}).
		Owns(&batchv1.Job{}).
		Owns(&corev1.Secret{}).
		WatchesRawSource(
			source.Kind(
				manager.GetCache(), client.Object(&corev1.ConfigMap{}),
				handler.TypedEnqueueRequestsFromMapFunc(mapConfigMap),
			),
		).
		Named("clientextension").
		Complete(reconciler)
}

// applyExtProvision publishes one ConfigMap per addressing bucket into the
// Liferay namespace. These carry no owner reference: owner references must be
// same-namespace, and a cross-namespace one causes the garbage collector to
// delete the dependent. Cleanup is the finalizer's job.
func (reconciler *ClientExtensionReconciler) applyExtProvision(
	context context.Context, clientExtension *cxv1alpha1.ClientExtension, liferayNamespace string,
) error {
	payload, buildError := BuildPayload(clientExtension)

	if buildError != nil {
		return buildError
	}

	var names []string

	for _, bucket := range []string{BucketInternal, BucketPublic} {
		document, found := payload.Buckets[bucket]

		name := ExtProvisionName(clientExtension, bucket)

		if !found {
			var existing corev1.ConfigMap

			if getError := reconciler.Get(
				context, types.NamespacedName{Name: name, Namespace: liferayNamespace}, &existing,
			); getError == nil {
				if deleteError := reconciler.Delete(context, &existing); deleteError != nil {
					return deleteError
				}
			}

			continue
		}

		domain := DomainFor(clientExtension, bucket)

		if bucket == BucketInternal {
			domain = InternalAddress(clientExtension)
		}

		configMap := &corev1.ConfigMap{
			ObjectMeta: metav1.ObjectMeta{Name: name, Namespace: liferayNamespace},
		}

		// Update in place. Liferay correlates its configurations with the
		// ConfigMap UID, so a delete and recreate tears down the OAuth2
		// application and issues a new client secret.
		if _, updateError := controllerutil.CreateOrUpdate(context, reconciler.Client, configMap, func() error {
			if configMap.Annotations == nil {
				configMap.Annotations = map[string]string{}
			}

			if domain != "" {
				configMap.Annotations[AnnotationDomains] = domain
				configMap.Annotations[AnnotationMainDomain] = domain
			}

			configMap.Data = map[string]string{
				clientExtension.Spec.ServiceID + ".client-extension-config.json": document,
			}

			configMap.Labels = map[string]string{
				LabelMetadataType:    MetadataTypeExtProvision,
				LabelOwnerName:       clientExtension.Name,
				LabelOwnerNamespace:  clientExtension.Namespace,
				LabelProjectName:     ProjectName(clientExtension),
				LabelServiceID:       clientExtension.Spec.ServiceID,
				LabelVirtualInstance: clientExtension.Spec.VirtualInstanceID,
			}

			return nil
		}); updateError != nil {
			return updateError
		}

		names = append(names, name)
	}

	clientExtension.Status.ExtProvisionConfigMapNames = names

	return nil
}

// findExtInit locates the ConfigMap Liferay wrote back. The name is never
// reconstructed: it is computed inside Liferay from the serviceId label, and
// guessing it is how the prototype chart silently mismounts.
func (reconciler *ClientExtensionReconciler) findExtInit(
	context context.Context, clientExtension *cxv1alpha1.ClientExtension, liferayNamespace string,
) (*corev1.ConfigMap, error) {
	var list corev1.ConfigMapList

	if error := reconciler.List(
		context, &list,
		client.InNamespace(liferayNamespace),
		client.MatchingLabels{
			LabelMetadataType:    MetadataTypeExtInit,
			LabelServiceID:       clientExtension.Spec.ServiceID,
			LabelVirtualInstance: clientExtension.Spec.VirtualInstanceID,
		},
	); error != nil {
		return nil, error
	}

	for index := range list.Items {
		if len(list.Items[index].Data) == 0 {
			continue
		}

		return &list.Items[index], nil
	}

	return nil, nil
}

// mirrorExtInit copies the ext-init payload into a Secret in the client
// extension's namespace. A Secret rather than a ConfigMap because that payload
// carries plaintext OAuth2 client secrets, and mirroring across a namespace
// boundary moves credentials into a tenant namespace.
func (reconciler *ClientExtensionReconciler) mirrorExtInit(
	context context.Context, clientExtension *cxv1alpha1.ClientExtension, extInit *corev1.ConfigMap,
) (string, error) {
	name := ExtInitSecretName(clientExtension)

	secret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Name: name, Namespace: clientExtension.Namespace},
	}

	if _, updateError := controllerutil.CreateOrUpdate(context, reconciler.Client, secret, func() error {
		if secret.Annotations == nil {
			secret.Annotations = map[string]string{}
		}

		secret.Annotations[AnnotationSource] = extInit.Namespace + "/" + extInit.Name

		secret.Labels = map[string]string{
			LabelMirror:          "true",
			LabelServiceID:       clientExtension.Spec.ServiceID,
			LabelVirtualInstance: clientExtension.Spec.VirtualInstanceID,
		}

		secret.StringData = map[string]string{}

		for key, value := range extInit.Data {
			secret.StringData[key] = value
		}

		return controllerutil.SetControllerReference(clientExtension, secret, reconciler.Scheme())
	}); updateError != nil {
		return "", updateError
	}

	return name, nil
}

// ensureDXPMetadataMirror makes the virtual instance's routes readable from the
// client extension's namespace. The mirror is shared by every client extension
// in that namespace bound to the same virtual instance, so ownership is
// expressed with one non-controller owner reference per client extension and
// the garbage collector removes it when the last one goes.
func (reconciler *ClientExtensionReconciler) ensureDXPMetadataMirror(
	context context.Context, clientExtension *cxv1alpha1.ClientExtension, source *corev1.ConfigMap,
) (string, error) {
	name := DXPMetadataName(clientExtension.Spec.VirtualInstanceID)
	sourceReference := source.Namespace + "/" + source.Name

	var existing corev1.ConfigMap

	getError := reconciler.Get(
		context, types.NamespacedName{Name: name, Namespace: clientExtension.Namespace}, &existing,
	)

	if getError == nil {
		if existing.Labels[LabelMirror] != "true" {
			return "", fmt.Errorf(
				"ConfigMap %q in namespace %q is not a mirror owned by this operator",
				name, clientExtension.Namespace,
			)
		}

		if existing.Annotations[AnnotationSource] != sourceReference {
			return "", fmt.Errorf(
				"ConfigMap %q already mirrors %q; refusing to repoint it at %q",
				name, existing.Annotations[AnnotationSource], sourceReference,
			)
		}
	} else if !apierrors.IsNotFound(getError) {
		return "", getError
	}

	mirror := &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{Name: name, Namespace: clientExtension.Namespace},
	}

	if _, updateError := controllerutil.CreateOrUpdate(context, reconciler.Client, mirror, func() error {
		if mirror.Annotations == nil {
			mirror.Annotations = map[string]string{}
		}

		mirror.Annotations[AnnotationSource] = sourceReference

		// The metadataType label is deliberately not copied. A live looking
		// dxp label on a mirror invites a co-located Liferay's virtual
		// instance custodian to delete it.
		mirror.Labels = map[string]string{
			LabelMirror:          "true",
			LabelVirtualInstance: clientExtension.Spec.VirtualInstanceID,
		}

		mirror.Data = map[string]string{}

		for key, value := range source.Data {
			mirror.Data[key] = value
		}

		return appendNonControllerOwner(clientExtension, mirror, reconciler.Scheme())
	}); updateError != nil {
		return "", updateError
	}

	return name, nil
}

func (reconciler *ClientExtensionReconciler) applyWorkload(
	context context.Context, clientExtension *cxv1alpha1.ClientExtension, sources WorkloadSources,
) (bool, error) {
	desired, error := BuildWorkload(clientExtension, sources)

	if error != nil {
		return false, error
	}

	if desired == nil {
		clientExtension.Status.WorkloadName = ""

		return true, nil
	}

	if error := controllerutil.SetControllerReference(
		clientExtension, desired, reconciler.Scheme(),
	); error != nil {
		return false, error
	}

	// A Job's pod template is immutable, so an existing one is left alone.
	// Rolling a batch client extension forward means deleting the Job, which
	// is a deliberate act rather than a side effect of reconciliation.
	if clientExtension.Spec.Workload.Kind == cxv1alpha1.WorkloadKindJob {
		var existing batchv1.Job

		if getError := reconciler.Get(
			context,
			types.NamespacedName{Name: desired.GetName(), Namespace: desired.GetNamespace()},
			&existing,
		); getError == nil {
			clientExtension.Status.WorkloadName = desired.GetName()

			return existing.Status.Succeeded > 0, nil
		} else if !apierrors.IsNotFound(getError) {
			return false, getError
		}
	}

	if error := reconciler.Patch(
		context, desired, client.Apply, client.ForceOwnership, client.FieldOwner(fieldOwner),
	); error != nil {
		return false, error
	}

	clientExtension.Status.WorkloadName = desired.GetName()

	return reconciler.workloadAvailable(context, clientExtension)
}

func (reconciler *ClientExtensionReconciler) workloadAvailable(
	context context.Context, clientExtension *cxv1alpha1.ClientExtension,
) (bool, error) {
	name := types.NamespacedName{
		Name: clientExtension.Name, Namespace: clientExtension.Namespace,
	}

	switch clientExtension.Spec.Workload.Kind {
	case cxv1alpha1.WorkloadKindCronJob:
		var cronJob batchv1.CronJob

		if error := reconciler.Get(context, name, &cronJob); error != nil {
			return false, client.IgnoreNotFound(error)
		}

		return true, nil
	case cxv1alpha1.WorkloadKindDeployment:
		var deployment appsv1.Deployment

		if error := reconciler.Get(context, name, &deployment); error != nil {
			return false, client.IgnoreNotFound(error)
		}

		return deployment.Status.AvailableReplicas > 0, nil
	case cxv1alpha1.WorkloadKindJob:
		var job batchv1.Job

		if error := reconciler.Get(context, name, &job); error != nil {
			return false, client.IgnoreNotFound(error)
		}

		return job.Status.Succeeded > 0, nil
	}

	return false, nil
}

// finalize removes the configuration from Liferay's namespace and waits for
// Liferay to acknowledge by dropping the ext-init data. The wait is bounded:
// deletion cannot hang forever, but releasing early can orphan OAuth2
// applications, so an expired wait is recorded as an event.
func (reconciler *ClientExtensionReconciler) finalize(
	context context.Context, clientExtension *cxv1alpha1.ClientExtension,
) (controllerruntime.Result, error) {
	if !controllerutil.ContainsFinalizer(clientExtension, Finalizer) {
		return controllerruntime.Result{}, nil
	}

	liferayNamespace := LiferayNamespace(clientExtension)

	for _, bucket := range []string{BucketInternal, BucketPublic} {
		configMap := &corev1.ConfigMap{
			ObjectMeta: metav1.ObjectMeta{
				Name:      ExtProvisionName(clientExtension, bucket),
				Namespace: liferayNamespace,
			},
		}

		if error := reconciler.Delete(context, configMap); client.IgnoreNotFound(error) != nil {
			return controllerruntime.Result{}, error
		}
	}

	extInit, error := reconciler.findExtInit(context, clientExtension, liferayNamespace)

	if error != nil {
		return controllerruntime.Result{}, error
	}

	if extInit != nil && !reconciler.gracePeriodExpired(context, clientExtension) {
		return controllerruntime.Result{RequeueAfter: reconciler.requeueInterval()}, nil
	}

	if extInit != nil && reconciler.Recorder != nil {
		reconciler.Recorder.Eventf(
			clientExtension, corev1.EventTypeWarning, "UnprovisionNotAcknowledged",
			"Liferay did not withdraw %q within %s; releasing the finalizer. "+
				"OAuth2 applications may be orphaned in the virtual instance.",
			extInit.Name, reconciler.gracePeriod(),
		)
	}

	controllerutil.RemoveFinalizer(clientExtension, Finalizer)

	return controllerruntime.Result{}, reconciler.Update(context, clientExtension)
}

func (reconciler *ClientExtensionReconciler) gracePeriod() time.Duration {
	if reconciler.ProvisioningGracePeriod > 0 {
		return reconciler.ProvisioningGracePeriod
	}

	return 5 * time.Minute
}

func (reconciler *ClientExtensionReconciler) gracePeriodExpired(
	context context.Context, clientExtension *cxv1alpha1.ClientExtension,
) bool {
	started := clientExtension.Annotations[AnnotationDeletionStarted]

	if started == "" {
		if clientExtension.Annotations == nil {
			clientExtension.Annotations = map[string]string{}
		}

		clientExtension.Annotations[AnnotationDeletionStarted] = time.Now().UTC().Format(time.RFC3339)

		_ = reconciler.Update(context, clientExtension)

		return false
	}

	startedAt, error := time.Parse(time.RFC3339, started)

	if error != nil {
		return true
	}

	return time.Since(startedAt) > reconciler.gracePeriod()
}

func (reconciler *ClientExtensionReconciler) requeueInterval() time.Duration {
	if reconciler.RequeueInterval > 0 {
		return reconciler.RequeueInterval
	}

	return 15 * time.Second
}

// resolveEnvironment validates the target Liferay and the tenant's permission
// to attach to it. Consent is granted from the Liferay side so a tenant cannot
// provision OAuth2 applications against a Liferay it does not own.
func (reconciler *ClientExtensionReconciler) resolveEnvironment(
	context context.Context, clientExtension *cxv1alpha1.ClientExtension,
) (string, error) {
	liferayNamespace := LiferayNamespace(clientExtension)

	var environment licensingv1alpha1.LiferayEnvironment

	if error := reconciler.Get(
		context,
		types.NamespacedName{
			Name:      clientExtension.Spec.LiferayEnvironmentRef.Name,
			Namespace: liferayNamespace,
		},
		&environment,
	); error != nil {
		if apierrors.IsNotFound(error) {
			return "", fmt.Errorf(
				"LiferayEnvironment %q does not exist in namespace %q",
				clientExtension.Spec.LiferayEnvironmentRef.Name, liferayNamespace,
			)
		}

		return "", error
	}

	if liferayNamespace == clientExtension.Namespace {
		return liferayNamespace, nil
	}

	for _, permitted := range environment.Spec.ClientExtensionNamespaces {
		if permitted == clientExtension.Namespace {
			return liferayNamespace, nil
		}
	}

	return "", fmt.Errorf(
		"namespace %q is not listed in spec.clientExtensionNamespaces of LiferayEnvironment %q/%q",
		clientExtension.Namespace, liferayNamespace, environment.Name,
	)
}

func (reconciler *ClientExtensionReconciler) degrade(
	context context.Context, clientExtension *cxv1alpha1.ClientExtension,
	reason string, message string,
) (controllerruntime.Result, error) {
	setCondition(clientExtension, cxv1alpha1.ConditionDelivered, metav1.ConditionFalse, reason, message)
	setCondition(clientExtension, cxv1alpha1.ConditionReady, metav1.ConditionFalse, reason, message)

	clientExtension.Status.Phase = cxv1alpha1.PhaseDegraded

	if reconciler.Recorder != nil {
		reconciler.Recorder.Event(clientExtension, corev1.EventTypeWarning, reason, message)
	}

	return reconciler.requeue(context, clientExtension)
}

func (reconciler *ClientExtensionReconciler) requeue(
	context context.Context, clientExtension *cxv1alpha1.ClientExtension,
) (controllerruntime.Result, error) {
	clientExtension.Status.ObservedGeneration = clientExtension.Generation

	if error := reconciler.Status().Update(context, clientExtension); error != nil {
		return controllerruntime.Result{}, client.IgnoreNotFound(error)
	}

	return controllerruntime.Result{RequeueAfter: reconciler.requeueInterval()}, nil
}

func setCondition(
	clientExtension *cxv1alpha1.ClientExtension, conditionType string,
	status metav1.ConditionStatus, reason string, message string,
) {
	meta.SetStatusCondition(&clientExtension.Status.Conditions, metav1.Condition{
		Message:            message,
		ObservedGeneration: clientExtension.Generation,
		Reason:             reason,
		Status:             status,
		Type:               conditionType,
	})
}
