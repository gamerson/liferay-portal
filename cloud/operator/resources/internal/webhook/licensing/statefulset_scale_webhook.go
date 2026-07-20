// Package licensing holds the admission webhooks the licensing agent serves.
//
// The marker below drives config/webhook/manifests.yaml. failurePolicy=ignore
// is deliberate: an outage of this webhook must not block the API server, so it
// fails open and lets the reconciler's clamp remain the backstop.
//
// +kubebuilder:webhook:path=/validate-apps-v1-statefulset,mutating=false,failurePolicy=ignore,sideEffects=None,groups=apps,resources=statefulsets;statefulsets/scale,verbs=update,versions=v1,name=vstatefulsetscale.licensing.liferay.com,admissionReviewVersions=v1
package licensing

import (
	"context"
	"fmt"
	"net/http"

	appsv1 "k8s.io/api/apps/v1"
	autoscalingv1 "k8s.io/api/autoscaling/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/webhook/admission"

	licensingv1alpha1 "github.com/liferay/liferay-portal/cloud/operator/api/licensing/v1alpha1"
)

// WebhookPath is the URL the manager serves this handler on. It must match the
// path in the +kubebuilder:webhook marker below and the generated manifest.
const WebhookPath = "/validate-apps-v1-statefulset"

// StatefulSetScaleValidator rejects attempts to scale a Liferay workload above
// the licensed maxClusterNodes. It fails open on anything it cannot decide
// (no governing environment, unknown ceiling, lookup error) so a broken or
// uninformed webhook never freezes legitimate scaling — the reconciler's clamp
// remains the backstop.
type StatefulSetScaleValidator struct {
	Client  client.Client
	Decoder admission.Decoder
}

// Handle implements admission.Handler.
func (v *StatefulSetScaleValidator) Handle(
	ctx context.Context,
	req admission.Request,
) admission.Response {

	log := logf.FromContext(ctx)

	requested, name, err := v.requestedReplicas(req)

	if err != nil {
		return admission.Errored(http.StatusBadRequest, err)
	}

	if requested == nil {
		return admission.Allowed("no replica count in request")
	}

	max, found, err := v.licensedMax(ctx, req.Namespace, name)

	if err != nil {
		// Fail open: never block scaling because the lookup failed.

		log.Error(
			err,
			"Licensed-ceiling lookup failed; allowing scale",
			"namespace", req.Namespace,
			"statefulset", name,
		)

		return admission.Allowed("licensed ceiling unavailable")
	}

	if !found || max <= 0 {
		return admission.Allowed("no licensed ceiling known for this workload")
	}

	if *requested > max {
		return admission.Denied(
			fmt.Sprintf(
				"replicas %d exceeds licensed maxClusterNodes %d for "+
					"StatefulSet %q; the extra Liferay node would fail "+
					"license validation on startup",
				*requested, max, name,
			),
		)
	}

	return admission.Allowed("within licensed node ceiling")
}

// requestedReplicas extracts the desired replica count and the StatefulSet name
// from either a scale-subresource request or a full StatefulSet update.
func (v *StatefulSetScaleValidator) requestedReplicas(
	req admission.Request,
) (*int32, string, error) {

	if req.SubResource == "scale" {
		scale := &autoscalingv1.Scale{}

		if err := v.Decoder.Decode(req, scale); err != nil {
			return nil, "", err
		}

		replicas := scale.Spec.Replicas

		return &replicas, req.Name, nil
	}

	statefulSet := &appsv1.StatefulSet{}

	if err := v.Decoder.Decode(req, statefulSet); err != nil {
		return nil, "", err
	}

	return statefulSet.Spec.Replicas, statefulSet.Name, nil
}

// licensedMax returns the maxClusterNodes for the LiferayEnvironment that
// governs the named StatefulSet in the namespace, if one exists.
func (v *StatefulSetScaleValidator) licensedMax(
	ctx context.Context,
	namespace string,
	workloadName string,
) (int32, bool, error) {

	list := &licensingv1alpha1.LiferayEnvironmentList{}

	if err := v.Client.List(
		ctx, list, client.InNamespace(namespace),
	); err != nil {

		return 0, false, err
	}

	for i := range list.Items {
		ref := list.Items[i].Spec.WorkloadRef

		if ref.Kind == "StatefulSet" && ref.Name == workloadName {
			return list.Items[i].Status.License.MaxClusterNodes, true, nil
		}
	}

	return 0, false, nil
}
