package licensing

import (
	"context"
	"testing"

	licensingv1alpha1 "github.com/liferay/liferay-portal/cloud/operator/api/licensing/v1alpha1"
	corev1 "k8s.io/api/core/v1"
	errors "k8s.io/apimachinery/pkg/api/errors"
	meta "k8s.io/apimachinery/pkg/api/meta"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	types "k8s.io/apimachinery/pkg/types"
	event "sigs.k8s.io/controller-runtime/pkg/event"
)

func TestDXPReportChanged(t *testing.T) {
	testCases := map[string]struct {
		activationMode string
		expected       bool
		update         func(liferayEnvironment *licensingv1alpha1.LiferayEnvironment)
	}{
		"a condition DXP reports is a change": {
			activationMode: activationModeDXP,
			expected:       true,
			update: func(liferayEnvironment *licensingv1alpha1.LiferayEnvironment) {
				meta.SetStatusCondition(
					&liferayEnvironment.Status.Conditions,
					metav1.Condition{
						Reason: "Unreachable",
						Status: metav1.ConditionFalse,
						Type:   conditionProvisioningReachable,
					},
				)
			},
		},
		"a license DXP reports is a change": {
			activationMode: activationModeDXP,
			expected:       true,
			update: func(liferayEnvironment *licensingv1alpha1.LiferayEnvironment) {
				liferayEnvironment.Status.License.MaxClusterNodes = pointerInt32(5)
			},
		},
		"a license the operator writes in its own mode is not a change": {
			activationMode: "Operator",
			expected:       false,
			update: func(liferayEnvironment *licensingv1alpha1.LiferayEnvironment) {
				liferayEnvironment.Status.License.MaxClusterNodes = pointerInt32(5)
			},
		},
		"a status the operator owns is not a change": {
			activationMode: activationModeDXP,
			expected:       false,
			update: func(liferayEnvironment *licensingv1alpha1.LiferayEnvironment) {
				liferayEnvironment.Status.EffectiveReplicas = pointerInt32(2)
				liferayEnvironment.Status.Phase = "Ready"
				liferayEnvironment.Status.ReplicaCeiling = pointerInt32(2)

				meta.SetStatusCondition(
					&liferayEnvironment.Status.Conditions,
					metav1.Condition{
						Reason: "WithinLicensedLimit",
						Status: metav1.ConditionTrue,
						Type:   conditionReplicasCountValid,
					},
				)
			},
		},
	}

	for name, testCase := range testCases {
		t.Run(name, func(t *testing.T) {
			oldLiferayEnvironment := dxpEnvironment(
				[]metav1.Condition{dxpCondition(conditionActivated, metav1.ConditionTrue)},
				pointerInt32(3),
			)

			oldLiferayEnvironment.Spec.ActivationMode = testCase.activationMode

			newLiferayEnvironment := oldLiferayEnvironment.DeepCopy()

			testCase.update(newLiferayEnvironment)

			actual := dxpReportChanged(
				event.UpdateEvent{
					ObjectNew: newLiferayEnvironment,
					ObjectOld: oldLiferayEnvironment,
				},
			)

			if actual != testCase.expected {
				t.Errorf("dxpReportChanged() = %t, want %t", actual, testCase.expected)
			}
		})
	}
}

func TestReconcileDXPActivationEnforcesTheReportedCeiling(t *testing.T) {
	testCases := map[string]struct {
		conditions             []metav1.Condition
		expectedPhase          string
		expectedReplicaCeiling int32
		maxClusterNodes        *int32
	}{
		"a reported maximum caps the workload": {
			conditions: []metav1.Condition{
				dxpCondition(conditionActivated, metav1.ConditionTrue),
				dxpCondition(conditionLicenseValid, metav1.ConditionTrue),
				dxpCondition(conditionProvisioningReachable, metav1.ConditionTrue),
			},
			expectedPhase:          "Ready",
			expectedReplicaCeiling: 2,
			maxClusterNodes:        pointerInt32(2),
		},
		"a reported maximum of zero keeps one replica running": {
			conditions: []metav1.Condition{
				dxpCondition(conditionActivated, metav1.ConditionTrue),
				dxpCondition(conditionLicenseValid, metav1.ConditionFalse),
			},
			expectedPhase:          "Degraded",
			expectedReplicaCeiling: 1,
			maxClusterNodes:        pointerInt32(0),
		},
		"an environment DXP has not activated runs one replica": {
			expectedPhase:          "Pending",
			expectedReplicaCeiling: 1,
		},
		"an unreachable provisioning server degrades without lowering the ceiling": {
			conditions: []metav1.Condition{
				dxpCondition(conditionActivated, metav1.ConditionTrue),
				dxpCondition(conditionLicenseValid, metav1.ConditionTrue),
				dxpCondition(conditionProvisioningReachable, metav1.ConditionFalse),
			},
			expectedPhase:          "Degraded",
			expectedReplicaCeiling: 3,
			maxClusterNodes:        pointerInt32(3),
		},
	}

	for name, testCase := range testCases {
		t.Run(name, func(t *testing.T) {
			objects := developmentObjects()

			objects[len(objects)-1] = dxpEnvironment(
				testCase.conditions, testCase.maxClusterNodes,
			)

			stubProvisioning := &stubProvisioning{}

			liferayEnvironmentReconciler, _ := reconcileEnvironment(
				stubProvisioning, t, objects...,
			)

			if stubProvisioning.activateCalled || stubProvisioning.manifestCalled {
				t.Error("Expected no provisioning calls in DXP mode")
			}

			liferayEnvironment := getEnvironment(liferayEnvironmentReconciler, t)

			assertReplicasEqual(
				liferayEnvironment.Status.ReplicaCeiling,
				&testCase.expectedReplicaCeiling,
				"status.replicaCeiling",
				t,
			)

			assertReplicasEqual(
				getStatefulSet(liferayEnvironmentReconciler, t).Spec.Replicas,
				&testCase.expectedReplicaCeiling,
				"statefulSet.spec.replicas",
				t,
			)

			assertReplicasEqual(
				liferayEnvironment.Status.License.MaxClusterNodes,
				testCase.maxClusterNodes,
				"status.license.maxClusterNodes",
				t,
			)

			if liferayEnvironment.Status.Phase != testCase.expectedPhase {
				t.Errorf(
					"Phase = %q, want %q",
					liferayEnvironment.Status.Phase, testCase.expectedPhase,
				)
			}

			for _, condition := range testCase.conditions {
				if !meta.IsStatusConditionPresentAndEqual(
					liferayEnvironment.Status.Conditions, condition.Type,
					condition.Status,
				) {
					t.Errorf("Expected the %s condition DXP reported to be kept", condition.Type)
				}
			}

			getError := liferayEnvironmentReconciler.Get(
				context.Background(),
				types.NamespacedName{
					Name:      "dev" + identitySecretSuffix,
					Namespace: "liferay-dev",
				},
				&corev1.Secret{},
			)

			if !errors.IsNotFound(getError) {
				t.Errorf("Expected no identity secret in DXP mode, got %v", getError)
			}
		})
	}
}

func dxpCondition(conditionType string, status metav1.ConditionStatus) metav1.Condition {
	return metav1.Condition{
		LastTransitionTime: metav1.Now(),
		Reason:             "ReportedByDXP",
		Status:             status,
		Type:               conditionType,
	}
}

func dxpEnvironment(
	conditions []metav1.Condition,
	maxClusterNodes *int32,
) *licensingv1alpha1.LiferayEnvironment {
	return &licensingv1alpha1.LiferayEnvironment{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "dev",
			Namespace: "liferay-dev",
		},
		Spec: licensingv1alpha1.LiferayEnvironmentSpec{
			ActivationMode:  activationModeDXP,
			DesiredReplicas: pointerInt32(3),
			WorkloadRef: licensingv1alpha1.WorkloadRef{
				Name: "dev-liferay",
			},
		},
		Status: licensingv1alpha1.LiferayEnvironmentStatus{
			Conditions: conditions,
			License: licensingv1alpha1.LicenseStatus{
				MaxClusterNodes: maxClusterNodes,
			},
		},
	}
}
