package licensing

import (
	"context"

	licensingv1alpha1 "github.com/liferay/liferay-portal/cloud/operator/api/licensing/v1alpha1"
	equality "k8s.io/apimachinery/pkg/api/equality"
	meta "k8s.io/apimachinery/pkg/api/meta"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	controllerruntime "sigs.k8s.io/controller-runtime"
	event "sigs.k8s.io/controller-runtime/pkg/event"
	predicate "sigs.k8s.io/controller-runtime/pkg/predicate"
)

const (
	activationModeDXP = "DXP"

	// DXP activates and renews the license from inside the workload, so the
	// workload must keep running to recover from any license state. The
	// ceiling never drops below one replica; DXP itself refuses service while
	// it is unlicensed.

	dxpMinimumReplicaCeiling = 1
)

func dxpPhase(conditions []metav1.Condition) string {
	activatedCondition := meta.FindStatusCondition(conditions, conditionActivated)

	if activatedCondition == nil {
		return "Pending"
	}

	if activatedCondition.Status != metav1.ConditionTrue ||
		!meta.IsStatusConditionTrue(conditions, conditionLicenseValid) ||
		meta.IsStatusConditionFalse(conditions, conditionProvisioningReachable) {

		return "Degraded"
	}

	return "Ready"
}

func dxpReplicaCeiling(
	liferayEnvironment *licensingv1alpha1.LiferayEnvironment,
) int32 {
	maxClusterNodes := liferayEnvironment.Status.License.MaxClusterNodes

	if maxClusterNodes == nil ||
		!meta.IsStatusConditionTrue(
			liferayEnvironment.Status.Conditions, conditionActivated,
		) {

		return dxpMinimumReplicaCeiling
	}

	return max(*maxClusterNodes, dxpMinimumReplicaCeiling)
}

// A status write does not bump the generation, so the status DXP reports
// would otherwise never wake the reconciler. Only the fields DXP owns count,
// and only in DXP mode, because the operator rewrites its own fields, and in
// operator mode the license too, on every reconcile.

func dxpReportChanged(updateEvent event.UpdateEvent) bool {
	newLiferayEnvironment, ok := updateEvent.ObjectNew.(*licensingv1alpha1.LiferayEnvironment)

	if !ok || newLiferayEnvironment.Spec.ActivationMode != activationModeDXP {
		return false
	}

	oldLiferayEnvironment, ok := updateEvent.ObjectOld.(*licensingv1alpha1.LiferayEnvironment)

	if !ok {
		return false
	}

	if !equality.Semantic.DeepEqual(
		oldLiferayEnvironment.Status.ActivatedAt,
		newLiferayEnvironment.Status.ActivatedAt,
	) ||
		!equality.Semantic.DeepEqual(
			oldLiferayEnvironment.Status.License,
			newLiferayEnvironment.Status.License,
		) {

		return true
	}

	for _, conditionType := range dxpConditionTypes {
		if !equality.Semantic.DeepEqual(
			meta.FindStatusCondition(
				oldLiferayEnvironment.Status.Conditions, conditionType,
			),
			meta.FindStatusCondition(
				newLiferayEnvironment.Status.Conditions, conditionType,
			),
		) {

			return true
		}
	}

	return false
}

func (liferayEnvironmentReconciler *LiferayEnvironmentReconciler) reconcileDXPActivation(
	context context.Context,
	liferayEnvironment *licensingv1alpha1.LiferayEnvironment,
) (controllerruntime.Result, error) {
	requeueAfter, error := liferayEnvironmentReconciler.enforceReplicaCeiling(
		context, liferayEnvironment, dxpReplicaCeiling(liferayEnvironment),
	)

	if error != nil {
		return controllerruntime.Result{}, error
	}

	liferayEnvironment.Status.Phase = dxpPhase(liferayEnvironment.Status.Conditions)

	if requeueAfter == 0 {
		requeueAfter = liferayEnvironmentReconciler.HeartbeatInterval
	}

	return liferayEnvironmentReconciler.finishAfter(
		context, liferayEnvironment, requeueAfter,
	)
}

var (
	dxpConditionTypes = []string{
		conditionActivated,
		conditionLicenseValid,
		conditionProvisioningReachable,
	}

	dxpReportChangedPredicate = predicate.Funcs{UpdateFunc: dxpReportChanged}
)
