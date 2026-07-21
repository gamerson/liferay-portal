// Package licensing holds the admission webhooks the licensing agent serves.
//
// The marker below drives config/webhook/manifests.yaml. failurePolicy=fail
// makes enforcement fail-closed (AC #6): if the webhook cannot confirm the
// licensed ceiling for a governed workload, the request is denied. This is only
// safe because the deployed webhook is scoped by a namespaceSelector (set in the
// Helm chart) to Liferay environment namespaces, so a webhook outage cannot
// freeze StatefulSets elsewhere. A namespaceSelector (not objectSelector) is
// required because the scale subresource admits a Scale object that does not
// carry the StatefulSet's labels. It fires on create and update so a fresh
// over-licensed workload is rejected, not only a scale-up.
//
// +kubebuilder:webhook:path=/validate-apps-v1-statefulset,mutating=false,failurePolicy=fail,sideEffects=None,groups=apps,resources=statefulsets;statefulsets/scale,verbs=create;update,versions=v1,name=vstatefulsetscale.licensing.liferay.com,admissionReviewVersions=v1
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

	// This webhook is scoped by namespaceSelector to Liferay environment
	// namespaces, so it fires for every StatefulSet operation (including the
	// scale subresource) in them. A StatefulSet governed by a LiferayEnvironment
	// is enforced fail-CLOSED: any inability to confirm its ceiling denies the
	// request. A StatefulSet that no LiferayEnvironment references is not a
	// licensed workload and is allowed through.

	max, found, err := v.licensedMax(ctx, req.Namespace, name)

	if err != nil {
		log.Error(
			err, "Licensed-ceiling lookup failed; denying (fail-closed)",
			"namespace", req.Namespace, "statefulset", name,
		)

		return admission.Denied(
			fmt.Sprintf(
				"cannot determine the licensed node ceiling for StatefulSet "+
					"%q (license lookup failed): %v",
				name, err,
			),
		)
	}

	if !found {
		return admission.Allowed("not a licensed Liferay workload")
	}

	if max <= 0 {
		return admission.Denied(
			fmt.Sprintf(
				"the licensed node ceiling for StatefulSet %q is not yet "+
					"available (license missing or not yet activated); "+
					"retry once the LiferayEnvironment reports maxClusterNodes",
				name,
			),
		)
	}

	if requested > max {
		return admission.Denied(
			fmt.Sprintf(
				"replicas %d exceeds licensed maxClusterNodes %d for "+
					"StatefulSet %q; the extra Liferay node would fail "+
					"license validation on startup",
				requested, max, name,
			),
		)
	}

	return admission.Allowed("within licensed node ceiling")
}

// requestedReplicas extracts the desired replica count and the StatefulSet name
// from either a scale-subresource request or a full StatefulSet create/update.
// A nil replica count defaults to 1, the StatefulSet default, so it is still
// enforced rather than waved through.
func (v *StatefulSetScaleValidator) requestedReplicas(
	req admission.Request,
) (int32, string, error) {

	if req.SubResource == "scale" {
		scale := &autoscalingv1.Scale{}

		if err := v.Decoder.Decode(req, scale); err != nil {
			return 0, "", err
		}

		return scale.Spec.Replicas, req.Name, nil
	}

	statefulSet := &appsv1.StatefulSet{}

	if err := v.Decoder.Decode(req, statefulSet); err != nil {
		return 0, "", err
	}

	replicas := int32(1)

	if statefulSet.Spec.Replicas != nil {
		replicas = *statefulSet.Spec.Replicas
	}

	return replicas, statefulSet.Name, nil
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
