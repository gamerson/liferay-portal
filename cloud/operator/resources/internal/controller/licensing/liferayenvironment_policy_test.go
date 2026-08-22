package licensing

import (
	"bytes"
	"context"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"testing"
	"time"

	licensingv1alpha1 "github.com/liferay/liferay-portal/cloud/operator/api/licensing/v1alpha1"
	addon "github.com/liferay/liferay-portal/cloud/operator/internal/addon"
	provisioning "github.com/liferay/liferay-portal/cloud/operator/internal/provisioning"
	appsv1 "k8s.io/api/apps/v1"
	autoscalingv1 "k8s.io/api/autoscaling/v1"
	corev1 "k8s.io/api/core/v1"
	errors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	unstructured "k8s.io/apimachinery/pkg/apis/meta/v1/unstructured"
	types "k8s.io/apimachinery/pkg/types"
	yaml "k8s.io/apimachinery/pkg/util/yaml"
	rest "k8s.io/client-go/rest"
	record "k8s.io/client-go/tools/record"
	controllerruntime "sigs.k8s.io/controller-runtime"
	client "sigs.k8s.io/controller-runtime/pkg/client"
	envtest "sigs.k8s.io/controller-runtime/pkg/envtest"
)

const chartDir = "../../../../../helm/dxp-operator"

const operatorUsername = "system:serviceaccount:liferay-system:liferay-dxp-operator"

func TestAdmissionPolicyDeniesOverLimitWrites(t *testing.T) {
	config := startPolicyEnvironment(t)

	testCases := map[string]struct {
		labelNamespace   bool
		maxClusterNodes  *int32
		namespaceName    string
		scaleSubresource bool
		wantDenied       bool
	}{
		"a licensed namespace denies an over limit scale": {
			labelNamespace:   true,
			maxClusterNodes:  pointerInt32(1),
			namespaceName:    "liferay-scale",
			scaleSubresource: true,
			wantDenied:       true,
		},
		"a licensed namespace denies an over limit write": {
			labelNamespace:  true,
			maxClusterNodes: pointerInt32(1),
			namespaceName:   "liferay-licensed",
			wantDenied:      true,
		},
		"a namespace the policy does not gate allows any write": {
			labelNamespace:  false,
			maxClusterNodes: pointerInt32(1),
			namespaceName:   "liferay-ungated",
			wantDenied:      false,
		},
		"an environment without a ceiling allows any write": {
			labelNamespace:  true,
			maxClusterNodes: nil,
			namespaceName:   "liferay-unlicensed",
			wantDenied:      false,
		},
	}

	for name, testCase := range testCases {
		t.Run(name, func(t *testing.T) {
			writeClient := newPolicyFixture(
				config, testCase.labelNamespace, testCase.maxClusterNodes,
				testCase.namespaceName, t,
			)

			var statefulSet appsv1.StatefulSet

			if error := writeClient.Get(
				context.Background(),
				types.NamespacedName{
					Name:      "dev-liferay",
					Namespace: testCase.namespaceName,
				},
				&statefulSet,
			); error != nil {
				t.Fatalf("Unable to read the workload: %v", error)
			}

			error := writeOverLimitReplicas(
				writeClient, &statefulSet, testCase.scaleSubresource,
			)

			if testCase.wantDenied {
				if !errors.IsForbidden(error) {
					t.Errorf("Write error = %v, want a forbidden response", error)
				} else if !strings.Contains(
					error.Error(), "ValidatingAdmissionPolicy",
				) {
					t.Errorf(
						"Write error = %v, want the admission policy to be the one refusing",
						error,
					)
				}
			}

			if !testCase.wantDenied && error != nil {
				t.Errorf("Write error = %v, want the write to be admitted", error)
			}
		})
	}
}

func TestReconcileAgainstTheAdmissionPolicy(t *testing.T) {
	config := startPolicyEnvironment(t)

	testCases := map[string]struct {
		impersonateOperator bool
		namespaceName       string
	}{
		"the exempt operator restores the ceiling": {
			impersonateOperator: true,
			namespaceName:       "liferay-exempt",
		},
		"the operator the policy validates restores the ceiling": {
			impersonateOperator: false,
			namespaceName:       "liferay-validated",
		},
	}

	for name, testCase := range testCases {
		t.Run(name, func(t *testing.T) {
			liferayEnvironmentReconciler := newPolicyReconciler(
				config, testCase.impersonateOperator, testCase.namespaceName, t,
			)

			_, error := liferayEnvironmentReconciler.Reconcile(
				context.Background(), controllerruntime.Request{
					NamespacedName: types.NamespacedName{
						Name:      "dev",
						Namespace: testCase.namespaceName,
					},
				},
			)

			if error != nil {
				t.Logf("The workload update was rejected: %v", error)
			}

			var liferayEnvironment licensingv1alpha1.LiferayEnvironment

			if error := liferayEnvironmentReconciler.Get(
				context.Background(),
				types.NamespacedName{
					Name:      "dev",
					Namespace: testCase.namespaceName,
				},
				&liferayEnvironment,
			); error != nil {
				t.Fatalf("Unable to read the environment: %v", error)
			}

			maxClusterNodes := liferayEnvironment.Status.License.MaxClusterNodes

			if maxClusterNodes == nil {
				t.Fatal(
					"License.MaxClusterNodes = <nil>, want the licensed 3 persisted so that the next attempt is admitted",
				)
			}

			if *maxClusterNodes != 3 {
				t.Errorf(
					"License.MaxClusterNodes = %d, want the licensed 3 persisted so that the next attempt is admitted",
					*maxClusterNodes,
				)
			}

			var statefulSet appsv1.StatefulSet

			if error := liferayEnvironmentReconciler.Get(
				context.Background(),
				types.NamespacedName{
					Name:      "dev-liferay",
					Namespace: testCase.namespaceName,
				},
				&statefulSet,
			); error != nil {
				t.Fatalf("Unable to read the workload: %v", error)
			}

			assertReplicasEqual(
				statefulSet.Spec.Replicas, pointerInt32(3), "Replicas", t,
			)
		})
	}
}

func startPolicyEnvironment(t *testing.T) *rest.Config {
	t.Helper()

	assetsDir := envtestAssetsDir(t)

	if assetsDir == "" {
		t.Skip(
			"Set KUBEBUILDER_ASSETS, or install the envtest binaries with setup-envtest, to run this test",
		)
	}

	if _, error := exec.LookPath("helm"); error != nil {
		t.Skip("Install helm to render the admission policy this test installs")
	}

	testEnvironment := &envtest.Environment{
		BinaryAssetsDirectory: assetsDir,
		CRDDirectoryPaths:     []string{filepath.Join(chartDir, "crds")},
		ErrorIfCRDPathMissing: true,
	}

	config, error := testEnvironment.Start()

	if error != nil {
		t.Fatalf("Unable to start the test environment: %v", error)
	}

	t.Cleanup(func() {
		if error := testEnvironment.Stop(); error != nil {
			t.Logf("Unable to stop the test environment: %v", error)
		}
	})

	installAdmissionPolicy(config, t)

	return config
}

func envtestAssetsDir(t *testing.T) string {
	t.Helper()

	if assetsDir := os.Getenv("KUBEBUILDER_ASSETS"); assetsDir != "" {
		return assetsDir
	}

	homeDir, error := os.UserHomeDir()

	if error != nil {
		return ""
	}

	matches, error := filepath.Glob(
		filepath.Join(homeDir, ".local/share/kubebuilder-envtest/k8s/*"),
	)

	if error != nil || len(matches) == 0 {
		return ""
	}

	return matches[len(matches)-1]
}

func installAdmissionPolicy(config *rest.Config, t *testing.T) {
	t.Helper()

	command := exec.Command(
		"helm", "template", "liferay-dxp-operator", chartDir,
		"--namespace", "liferay-system",
		"--show-only", "templates/validating-admission-policy.yaml",
	)

	rendered, error := command.Output()

	if error != nil {
		t.Fatalf("Unable to render the admission policy: %v", error)
	}

	policyClient, error := client.New(config, client.Options{})

	if error != nil {
		t.Fatalf("Unable to build a client: %v", error)
	}

	decoder := yaml.NewYAMLOrJSONDecoder(bytes.NewReader(rendered), 4096)

	for {
		var object unstructured.Unstructured

		if error := decoder.Decode(&object); error != nil {
			break
		}

		if len(object.Object) == 0 {
			continue
		}

		if error := policyClient.Create(
			context.Background(), &object,
		); error != nil {
			t.Fatalf(
				"Unable to install %s %s: %v",
				object.GetKind(), object.GetName(), error,
			)
		}
	}
}

func newPolicyFixture(
	config *rest.Config, labelNamespace bool, maxClusterNodes *int32,
	namespaceName string, t *testing.T,
) client.Client {
	t.Helper()

	setUpClient, error := client.New(config, client.Options{Scheme: newScheme(t)})

	if error != nil {
		t.Fatalf("Unable to build a client: %v", error)
	}

	labels := map[string]string{}

	if labelNamespace {
		labels[environmentLabel] = "true"
	}

	if error := setUpClient.Create(
		context.Background(),
		&corev1.Namespace{
			ObjectMeta: metav1.ObjectMeta{
				Labels: labels,
				Name:   namespaceName,
			},
		},
	); error != nil {
		t.Fatalf("Unable to create the namespace: %v", error)
	}

	liferayEnvironment := &licensingv1alpha1.LiferayEnvironment{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "dev",
			Namespace: namespaceName,
		},
		Spec: licensingv1alpha1.LiferayEnvironmentSpec{
			ActivationCodeSecretRef: licensingv1alpha1.SecretKeyRef{
				Key:  "activationCode",
				Name: "dev-activation",
			},
			DesiredReplicas: pointerInt32(3),
			WorkloadRef: licensingv1alpha1.WorkloadRef{
				Name: "dev-liferay",
			},
		},
	}

	if error := setUpClient.Create(
		context.Background(), liferayEnvironment,
	); error != nil {
		t.Fatalf("Unable to create the environment: %v", error)
	}

	liferayEnvironment.Status.License.MaxClusterNodes = maxClusterNodes

	if error := setUpClient.Status().Update(
		context.Background(), liferayEnvironment,
	); error != nil {
		t.Fatalf("Unable to write the ceiling: %v", error)
	}

	if error := setUpClient.Create(
		context.Background(), newWorkload(namespaceName),
	); error != nil {
		t.Fatalf("Unable to create the workload: %v", error)
	}

	return setUpClient
}

func newPolicyReconciler(
	config *rest.Config, impersonateOperator bool, namespaceName string,
	t *testing.T,
) *LiferayEnvironmentReconciler {
	t.Helper()

	setUpClient, error := client.New(config, client.Options{Scheme: newScheme(t)})

	if error != nil {
		t.Fatalf("Unable to build a client: %v", error)
	}

	namespace := &corev1.Namespace{
		ObjectMeta: metav1.ObjectMeta{
			Labels: map[string]string{environmentLabel: "true"},
			Name:   namespaceName,
		},
	}

	if error := setUpClient.Create(context.Background(), namespace); error != nil {
		t.Fatalf("Unable to create the namespace: %v", error)
	}

	activatedAt := metav1.Now()

	liferayEnvironment := &licensingv1alpha1.LiferayEnvironment{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "dev",
			Namespace: namespaceName,
		},
		Spec: licensingv1alpha1.LiferayEnvironmentSpec{
			ActivationCodeSecretRef: licensingv1alpha1.SecretKeyRef{
				Key:  "activationCode",
				Name: "dev-activation",
			},
			DesiredReplicas: pointerInt32(3),
			WorkloadRef: licensingv1alpha1.WorkloadRef{
				Name: "dev-liferay",
			},
		},
	}

	if error := setUpClient.Create(
		context.Background(), liferayEnvironment,
	); error != nil {
		t.Fatalf("Unable to create the environment: %v", error)
	}

	liferayEnvironment.Status.ActivatedAt = &activatedAt
	liferayEnvironment.Status.License.MaxClusterNodes = pointerInt32(0)

	if error := setUpClient.Status().Update(
		context.Background(), liferayEnvironment,
	); error != nil {
		t.Fatalf("Unable to write the blocked ceiling: %v", error)
	}

	if error := setUpClient.Create(
		context.Background(), newWorkload(namespaceName),
	); error != nil {
		t.Fatalf("Unable to create the workload: %v", error)
	}

	reconcilerConfig := rest.CopyConfig(config)

	if impersonateOperator {
		reconcilerConfig.Impersonate = rest.ImpersonationConfig{
			Groups:   []string{"system:masters"},
			UserName: operatorUsername,
		}
	}

	reconcilerClient, error := client.New(
		reconcilerConfig, client.Options{Scheme: newScheme(t)},
	)

	if error != nil {
		t.Fatalf("Unable to build the reconciler client: %v", error)
	}

	provisioningClient := &stubProvisioning{
		entitlements: &provisioning.Entitlements{
			LicenseXML: []byte(virtualClusterLicenseXML(
				"Friday, March 2, 2029 12:00:00 AM GMT", 3,
				string(namespace.UID),
			)),
			MaxClusterNodes: 3,
		},
	}

	return &LiferayEnvironmentReconciler{
		Client:               reconcilerClient,
		HeartbeatInterval:    10 * time.Minute,
		MarketplaceMountPath: t.TempDir(),
		Provisioning:         provisioningClient,
		Recorder:             record.NewFakeRecorder(10),
		Syncer: addon.NewSyncer(
			provisioningClient, 15*time.Second, 30*time.Second, 30*time.Minute,
			inlineRunner{},
		),
	}
}

func writeOverLimitReplicas(
	writeClient client.Client, statefulSet *appsv1.StatefulSet,
	scaleSubresource bool,
) error {
	if scaleSubresource {
		return writeClient.SubResource("scale").Update(
			context.Background(), statefulSet,
			client.WithSubResourceBody(
				&autoscalingv1.Scale{
					ObjectMeta: metav1.ObjectMeta{
						Name:      statefulSet.Name,
						Namespace: statefulSet.Namespace,
					},
					Spec: autoscalingv1.ScaleSpec{Replicas: 3},
				},
			),
		)
	}

	statefulSet.Spec.Replicas = pointerInt32(3)

	return writeClient.Update(context.Background(), statefulSet)
}

func newWorkload(namespaceName string) *appsv1.StatefulSet {
	labels := map[string]string{"app": "dev-liferay"}

	return &appsv1.StatefulSet{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "dev-liferay",
			Namespace: namespaceName,
		},
		Spec: appsv1.StatefulSetSpec{
			Replicas:    pointerInt32(0),
			Selector:    &metav1.LabelSelector{MatchLabels: labels},
			ServiceName: "dev-liferay",
			Template: corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{Labels: labels},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Image: "registry.k8s.io/pause:3.9",
							Name:  "liferay",
						},
					},
				},
			},
		},
	}
}
