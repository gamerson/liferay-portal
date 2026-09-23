package cx

import (
	"fmt"

	cxv1alpha1 "github.com/liferay/liferay-portal/cloud/operator/api/cx/v1alpha1"
	appsv1 "k8s.io/api/apps/v1"
	batchv1 "k8s.io/api/batch/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	client "sigs.k8s.io/controller-runtime/pkg/client"
)

// WorkloadSources names the objects the pod template is wired to.
type WorkloadSources struct {
	// DXPMetadataConfigMapName is the ConfigMap carrying the virtual
	// instance's routes, either Liferay's own or the mirror.
	DXPMetadataConfigMapName string

	// ExtInitSecretName is the Secret carrying the OAuth2 credentials.
	ExtInitSecretName string
}

// BuildWorkload renders the child object for a client extension. It returns
// nil when the client extension is configuration only.
func BuildWorkload(
	clientExtension *cxv1alpha1.ClientExtension, sources WorkloadSources,
) (client.Object, error) {
	workload := clientExtension.Spec.Workload

	if workload == nil {
		return nil, nil
	}

	template := corev1.PodTemplateSpec{
		ObjectMeta: metav1.ObjectMeta{
			Annotations: workload.Template.Metadata.Annotations,
			Labels:      workload.Template.Metadata.Labels,
		},
		Spec: *workload.Template.Spec.DeepCopy(),
	}

	injectPodTemplate(&template, clientExtension, sources)

	objectMeta := metav1.ObjectMeta{
		Labels:    WorkloadLabels(clientExtension),
		Name:      clientExtension.Name,
		Namespace: clientExtension.Namespace,
	}

	switch workload.Kind {
	case cxv1alpha1.WorkloadKindCronJob:
		template.Spec.RestartPolicy = restartPolicyOrDefault(template.Spec.RestartPolicy)

		return &batchv1.CronJob{
			ObjectMeta: objectMeta,
			TypeMeta:   metav1.TypeMeta{APIVersion: "batch/v1", Kind: "CronJob"},
			Spec: batchv1.CronJobSpec{
				ConcurrencyPolicy: batchv1.ForbidConcurrent,
				JobTemplate: batchv1.JobTemplateSpec{
					Spec: batchv1.JobSpec{
						BackoffLimit: workload.BackoffLimit,
						Template:     template,
					},
				},
				Schedule: workload.Schedule,
			},
		}, nil
	case cxv1alpha1.WorkloadKindDeployment:
		return &appsv1.Deployment{
			ObjectMeta: objectMeta,
			TypeMeta:   metav1.TypeMeta{APIVersion: "apps/v1", Kind: "Deployment"},
			Spec: appsv1.DeploymentSpec{
				Replicas: workload.Replicas,
				Selector: &metav1.LabelSelector{MatchLabels: SelectorLabels(clientExtension)},
				Template: template,
			},
		}, nil
	case cxv1alpha1.WorkloadKindJob:
		template.Spec.RestartPolicy = restartPolicyOrDefault(template.Spec.RestartPolicy)

		return &batchv1.Job{
			ObjectMeta: objectMeta,
			TypeMeta:   metav1.TypeMeta{APIVersion: "batch/v1", Kind: "Job"},
			Spec: batchv1.JobSpec{
				BackoffLimit: workload.BackoffLimit,
				Template:     template,
			},
		}, nil
	}

	return nil, fmt.Errorf("unsupported workload kind %q", workload.Kind)
}

// EmptyWorkload returns a typed empty object for a workload kind, for lookups
// and deletions.
func EmptyWorkload(kind string) (client.Object, error) {
	switch kind {
	case cxv1alpha1.WorkloadKindCronJob:
		return &batchv1.CronJob{}, nil
	case cxv1alpha1.WorkloadKindDeployment:
		return &appsv1.Deployment{}, nil
	case cxv1alpha1.WorkloadKindJob:
		return &batchv1.Job{}, nil
	}

	return nil, fmt.Errorf("unsupported workload kind %q", kind)
}

// SelectorLabels are the labels the chart's Service selects on. The operator
// adds to pod labels but never removes or rewrites what the chart set, so the
// chart keeps control of its own selector.
func SelectorLabels(clientExtension *cxv1alpha1.ClientExtension) map[string]string {
	return map[string]string{
		"app.kubernetes.io/instance": clientExtension.Name,
		"app.kubernetes.io/name":     clientExtension.Spec.ServiceID,
	}
}

// WorkloadLabels are the labels stamped on the child object itself.
func WorkloadLabels(clientExtension *cxv1alpha1.ClientExtension) map[string]string {
	var labels = map[string]string{}

	for key, value := range SelectorLabels(clientExtension) {
		labels[key] = value
	}

	labels["app.kubernetes.io/managed-by"] = "dxp-operator"
	labels[LabelServiceID] = clientExtension.Spec.ServiceID
	labels[LabelVirtualInstance] = clientExtension.Spec.VirtualInstanceID

	return labels
}

// injectPodTemplate wires the two metadata sources into every container and
// stamps the labels Liferay correlates on. It is additive on labels: a label
// the chart already placed is left alone.
func injectPodTemplate(
	template *corev1.PodTemplateSpec, clientExtension *cxv1alpha1.ClientExtension,
	sources WorkloadSources,
) {
	if template.Labels == nil {
		template.Labels = map[string]string{}
	}

	for key, value := range WorkloadLabels(clientExtension) {
		if _, found := template.Labels[key]; !found {
			template.Labels[key] = value
		}
	}

	template.Spec.Volumes = upsertVolume(template.Spec.Volumes, corev1.Volume{
		Name: VolumeDXP,
		VolumeSource: corev1.VolumeSource{
			ConfigMap: &corev1.ConfigMapVolumeSource{
				LocalObjectReference: corev1.LocalObjectReference{
					Name: sources.DXPMetadataConfigMapName,
				},
			},
		},
	})

	// A client extension with no OAuth2 application has no credentials to
	// mount. Liferay only writes ext-init for extensions that declare one.
	if sources.ExtInitSecretName != "" {
		template.Spec.Volumes = upsertVolume(template.Spec.Volumes, corev1.Volume{
			Name: VolumeExtInit,
			VolumeSource: corev1.VolumeSource{
				Secret: &corev1.SecretVolumeSource{SecretName: sources.ExtInitSecretName},
			},
		})
	}

	for index := range template.Spec.Containers {
		injectContainer(&template.Spec.Containers[index], sources)
	}

	for index := range template.Spec.InitContainers {
		injectContainer(&template.Spec.InitContainers[index], sources)
	}
}

func injectContainer(container *corev1.Container, sources WorkloadSources) {
	container.Env = upsertEnv(container.Env, corev1.EnvVar{
		Name: EnvRoutesClientExtension, Value: MountPathExtInit,
	})
	container.Env = upsertEnv(container.Env, corev1.EnvVar{
		Name: EnvRoutesDXP, Value: MountPathDXP,
	})

	container.VolumeMounts = upsertVolumeMount(container.VolumeMounts, corev1.VolumeMount{
		MountPath: MountPathDXP, Name: VolumeDXP, ReadOnly: true,
	})
	if sources.ExtInitSecretName != "" {
		container.VolumeMounts = upsertVolumeMount(container.VolumeMounts, corev1.VolumeMount{
			MountPath: MountPathExtInit, Name: VolumeExtInit, ReadOnly: true,
		})
	}
}

func restartPolicyOrDefault(policy corev1.RestartPolicy) corev1.RestartPolicy {
	if policy == "" {
		return corev1.RestartPolicyNever
	}

	return policy
}

func upsertEnv(values []corev1.EnvVar, value corev1.EnvVar) []corev1.EnvVar {
	for index := range values {
		if values[index].Name == value.Name {
			values[index] = value

			return values
		}
	}

	return append(values, value)
}

func upsertVolume(values []corev1.Volume, value corev1.Volume) []corev1.Volume {
	for index := range values {
		if values[index].Name == value.Name {
			values[index] = value

			return values
		}
	}

	return append(values, value)
}

func upsertVolumeMount(values []corev1.VolumeMount, value corev1.VolumeMount) []corev1.VolumeMount {
	for index := range values {
		if values[index].Name == value.Name {
			values[index] = value

			return values
		}
	}

	return append(values, value)
}
