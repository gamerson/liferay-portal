package cx_test

import (
	"strings"
	"testing"

	cx "github.com/liferay/liferay-portal/cloud/operator/internal/controller/cx"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
)

func wiredDeployment() *appsv1.Deployment {
	return &appsv1.Deployment{
		Spec: appsv1.DeploymentSpec{
			Template: corev1.PodTemplateSpec{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Env: []corev1.EnvVar{
								{Name: cx.EnvRoutesClientExtension, Value: cx.MountPathExtInit},
								{Name: cx.EnvRoutesDXP, Value: cx.MountPathDXP},
							},
							Name: "client-extension",
							VolumeMounts: []corev1.VolumeMount{
								{MountPath: cx.MountPathDXP, Name: cx.VolumeDXP},
								{MountPath: cx.MountPathExtInit, Name: cx.VolumeExtInit},
							},
						},
					},
					Volumes: []corev1.Volume{
						{
							Name: cx.VolumeDXP,
							VolumeSource: corev1.VolumeSource{
								ConfigMap: &corev1.ConfigMapVolumeSource{
									LocalObjectReference: corev1.LocalObjectReference{Name: "vi-lxc-dxp-metadata"},
								},
							},
						},
						{
							Name: cx.VolumeExtInit,
							VolumeSource: corev1.VolumeSource{
								Secret: &corev1.SecretVolumeSource{SecretName: "svc-lxc-ext-init"},
							},
						},
					},
				},
			},
		},
	}
}

var sources = cx.WorkloadSources{
	DXPMetadataConfigMapName: "vi-lxc-dxp-metadata",
	ExtInitSecretName:        "svc-lxc-ext-init",
}

func TestValidateWorkloadAcceptsAWiredTemplate(testing_ *testing.T) {
	issues, error := cx.ValidateWorkload(wiredDeployment(), sources)

	if error != nil {
		testing_.Fatal(error)
	}

	if len(issues) != 0 {
		testing_.Errorf("a correctly wired template must raise nothing, got %v", issues)
	}
}

func TestValidateWorkloadNamesTheMissingPiece(testing_ *testing.T) {
	deployment := wiredDeployment()
	deployment.Spec.Template.Spec.Volumes = deployment.Spec.Template.Spec.Volumes[:1]

	issues, error := cx.ValidateWorkload(deployment, sources)

	if error != nil {
		testing_.Fatal(error)
	}

	if len(issues) != 1 {
		testing_.Fatalf("expected one issue, got %v", issues)
	}

	// The whole point of validating rather than injecting is that the report
	// identifies the object to fix.
	if !strings.Contains(issues[0], "svc-lxc-ext-init") {
		testing_.Errorf("the issue must name the Secret, got %q", issues[0])
	}
}

// A client extension with no OAuth2 application never receives credentials, so
// requiring the mount would leave every frontend-only extension misconfigured.
func TestValidateWorkloadSkipsCredentialsWhenNoneAreIssued(testing_ *testing.T) {
	deployment := wiredDeployment()
	deployment.Spec.Template.Spec.Volumes = deployment.Spec.Template.Spec.Volumes[:1]
	deployment.Spec.Template.Spec.Containers[0].VolumeMounts =
		deployment.Spec.Template.Spec.Containers[0].VolumeMounts[:1]
	deployment.Spec.Template.Spec.Containers[0].Env =
		deployment.Spec.Template.Spec.Containers[0].Env[1:]

	issues, error := cx.ValidateWorkload(
		deployment, cx.WorkloadSources{DXPMetadataConfigMapName: "vi-lxc-dxp-metadata"},
	)

	if error != nil {
		testing_.Fatal(error)
	}

	if len(issues) != 0 {
		testing_.Errorf("no credentials means no mount is required, got %v", issues)
	}
}

func TestStampConfigDigestIsIdempotent(testing_ *testing.T) {
	deployment := wiredDeployment()

	stamped, error := cx.StampConfigDigest(deployment, "abc")

	if error != nil {
		testing_.Fatal(error)
	}

	if !stamped {
		testing_.Error("the first stamp must report a change")
	}

	if stamped, _ = cx.StampConfigDigest(deployment, "abc"); stamped {
		testing_.Error("restamping the same digest must not report a change, or every resync rolls the pods")
	}

	if stamped, _ = cx.StampConfigDigest(deployment, "def"); !stamped {
		testing_.Error("a changed digest must report a change")
	}
}
