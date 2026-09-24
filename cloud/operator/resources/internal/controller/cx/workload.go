package cx

import (
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"sort"

	cxv1alpha1 "github.com/liferay/liferay-portal/cloud/operator/api/cx/v1alpha1"
	appsv1 "k8s.io/api/apps/v1"
	batchv1 "k8s.io/api/batch/v1"
	corev1 "k8s.io/api/core/v1"
	client "sigs.k8s.io/controller-runtime/pkg/client"
)

// WorkloadSources names the objects the workload's pod template is expected to
// be wired to.
type WorkloadSources struct {
	// ConfigDigest fingerprints the content those objects carry. It is stamped
	// onto the pod template so that a change rolls the pods.
	ConfigDigest string

	// DXPMetadataConfigMapName is the ConfigMap carrying the virtual
	// instance's routes, either Liferay's own or the mirror.
	DXPMetadataConfigMapName string

	// ExtInitSecretName is the Secret carrying the OAuth2 credentials. It is
	// empty for a client extension that declares no OAuth2 application, which
	// Liferay never issues credentials for.
	ExtInitSecretName string
}

// EmptyWorkload returns a typed empty object for a workload kind, for lookups.
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

// PodTemplateOf returns the pod template a workload object carries, so that one
// caller can read or stamp it without switching on the kind.
func PodTemplateOf(workload client.Object) (*corev1.PodTemplateSpec, error) {
	switch typed := workload.(type) {
	case *appsv1.Deployment:
		return &typed.Spec.Template, nil
	case *batchv1.CronJob:
		return &typed.Spec.JobTemplate.Spec.Template, nil
	case *batchv1.Job:
		return &typed.Spec.Template, nil
	}

	return nil, fmt.Errorf("unsupported workload type %T", workload)
}

// WorkloadAvailable reports whether a workload is serving. A Job is available
// once it has succeeded; the others once a replica reports ready.
func WorkloadAvailable(workload client.Object) bool {
	switch typed := workload.(type) {
	case *appsv1.Deployment:
		return typed.Status.ReadyReplicas > 0
	case *batchv1.CronJob:
		// A CronJob runs on its schedule; there is nothing to wait for, and
		// treating "has not fired yet" as unready would leave every scheduled
		// client extension permanently pending.
		return true
	case *batchv1.Job:
		return typed.Status.Succeeded > 0
	}

	return false
}

// ValidateWorkload checks that the pod template is wired to the metadata the
// client extension runtime reads at startup.
//
// The operator deliberately reports rather than repairs. The workload belongs
// to the chart, so Helm and Argo CD own its spec; silently adding a mount here
// would be reverted on the next sync and would hide a chart that is wrong.
// Naming the missing piece is what lets someone fix it at the source.
func ValidateWorkload(
	workload client.Object, sources WorkloadSources,
) ([]string, error) {
	template, error := PodTemplateOf(workload)

	if error != nil {
		return nil, error
	}

	var issues []string

	if !hasConfigMapVolume(template.Spec.Volumes, sources.DXPMetadataConfigMapName) {
		issues = append(
			issues,
			fmt.Sprintf(
				"no volume mounts ConfigMap %q, so the pod cannot read the virtual instance routes",
				sources.DXPMetadataConfigMapName,
			),
		)
	}

	if sources.ExtInitSecretName != "" &&
		!hasSecretVolume(template.Spec.Volumes, sources.ExtInitSecretName) {

		issues = append(
			issues,
			fmt.Sprintf(
				"no volume mounts Secret %q, so the pod cannot read its OAuth2 credentials",
				sources.ExtInitSecretName,
			),
		)
	}

	containers := template.Spec.Containers

	if len(containers) == 0 {
		return append(issues, "the pod template declares no containers"), nil
	}

	for index := range containers {
		issues = append(issues, containerIssues(&containers[index], sources)...)
	}

	return issues, nil
}

// StampConfigDigest writes the digest onto the pod template and reports whether
// anything changed.
//
// This is the one field the operator writes on a workload it does not own.
// Kubernetes propagates a changed ConfigMap or Secret into a running container,
// but nothing rereads it -- every client extension runtime parses those files
// once at startup -- so a credential Liferay reissued would otherwise leave the
// pod authenticating with the previous one. Helm's three-way merge preserves a
// field absent from both the old and the new manifest, so the annotation
// survives an upgrade.
func StampConfigDigest(workload client.Object, digest string) (bool, error) {
	if digest == "" {
		return false, nil
	}

	template, error := PodTemplateOf(workload)

	if error != nil {
		return false, error
	}

	if template.Annotations[AnnotationConfigDigest] == digest {
		return false, nil
	}

	if template.Annotations == nil {
		template.Annotations = map[string]string{}
	}

	template.Annotations[AnnotationConfigDigest] = digest

	return true, nil
}

// ConfigDigest fingerprints the payloads a pod reads at startup. Keys are
// sorted so that the digest depends on content alone, and each key and value
// is length prefixed so that no two distinct maps can encode the same way.
func ConfigDigest(payloads ...map[string]string) string {
	hash := sha256.New()

	for _, payload := range payloads {
		keys := make([]string, 0, len(payload))

		for key := range payload {
			keys = append(keys, key)
		}

		sort.Strings(keys)

		for _, key := range keys {
			fmt.Fprintf(hash, "%d:%s=%d:%s\n", len(key), key, len(payload[key]), payload[key])
		}

		fmt.Fprintln(hash, "--")
	}

	return hex.EncodeToString(hash.Sum(nil))
}

func containerIssues(container *corev1.Container, sources WorkloadSources) []string {
	var issues []string

	for _, required := range []struct {
		name  string
		value string
	}{
		{EnvRoutesClientExtension, MountPathExtInit},
		{EnvRoutesDXP, MountPathDXP},
	} {
		if sources.ExtInitSecretName == "" && required.name == EnvRoutesClientExtension {
			continue
		}

		if !hasEnv(container.Env, required.name) {
			issues = append(
				issues,
				fmt.Sprintf(
					"container %q does not set %s", container.Name, required.name,
				),
			)
		}
	}

	if !hasMountPath(container.VolumeMounts, MountPathDXP) {
		issues = append(
			issues,
			fmt.Sprintf(
				"container %q does not mount %s", container.Name, MountPathDXP,
			),
		)
	}

	if sources.ExtInitSecretName != "" &&
		!hasMountPath(container.VolumeMounts, MountPathExtInit) {

		issues = append(
			issues,
			fmt.Sprintf(
				"container %q does not mount %s", container.Name, MountPathExtInit,
			),
		)
	}

	return issues
}

func hasConfigMapVolume(volumes []corev1.Volume, name string) bool {
	for index := range volumes {
		source := volumes[index].ConfigMap

		if (source != nil) && (source.Name == name) {
			return true
		}
	}

	return false
}

func hasEnv(values []corev1.EnvVar, name string) bool {
	for index := range values {
		if values[index].Name == name {
			return true
		}
	}

	return false
}

func hasMountPath(mounts []corev1.VolumeMount, path string) bool {
	for index := range mounts {
		if mounts[index].MountPath == path {
			return true
		}
	}

	return false
}

func hasSecretVolume(volumes []corev1.Volume, name string) bool {
	for index := range volumes {
		source := volumes[index].Secret

		if (source != nil) && (source.SecretName == name) {
			return true
		}
	}

	return false
}
