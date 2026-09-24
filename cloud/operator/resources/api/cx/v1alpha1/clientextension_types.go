package v1alpha1

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func init() {
	SchemeBuilder.Register(&ClientExtension{}, &ClientExtensionList{})
}

// Condition types reported on a ClientExtension.
const (
	// ConditionDelivered reports that the ext-provision ConfigMaps have been
	// applied into the Liferay namespace.
	ConditionDelivered = "Delivered"

	// ConditionConfigurationAccepted reports whether the virtual instance
	// applied the configuration payload. Liferay publishes the outcome as an
	// ext-status ConfigMap; without it a rejected payload is visible only in
	// the portal log while the client extension still looks delivered.
	ConditionConfigurationAccepted = "ConfigurationAccepted"

	// ConditionProvisioned reports that Liferay wrote back ext-init data for
	// this client extension.
	ConditionProvisioned = "Provisioned"

	// ConditionReady reports that the workload, if any, is available.
	ConditionReady = "Ready"

	// ConditionWorkloadAccepted reports whether the referenced workload was
	// found and is wired to the metadata this client extension needs. The
	// operator no longer builds the pod template, so this is where a workload
	// missing a mount is reported rather than silently corrected.
	ConditionWorkloadAccepted = "WorkloadAccepted"
)

// Workload kinds.
const (
	WorkloadKindCronJob    = "CronJob"
	WorkloadKindDeployment = "Deployment"
	WorkloadKindJob        = "Job"
)

// Phases reported on a ClientExtension.
const (
	PhaseDegraded = "Degraded"
	PhasePending  = "Pending"
	PhaseReady    = "Ready"
)

// LiferayEnvironmentRef identifies the Liferay this client extension attaches
// to. An empty Namespace means the ClientExtension's own namespace.
type LiferayEnvironmentRef struct {
	// +kubebuilder:validation:Required
	Name string `json:"name"`

	// +optional
	Namespace string `json:"namespace,omitempty"`
}

// ConfigurationError is one entry of the payload that the virtual instance
// refused, reported back by Liferay.
type ConfigurationError struct {
	// ConfigMapName is the ext-provision ConfigMap the entry came from.
	// +optional
	ConfigMapName string `json:"configMapName,omitempty"`

	// +optional
	Message string `json:"message,omitempty"`

	// Phase is Parse when the payload could not be read at all, and Apply when
	// a single configuration failed.
	// +kubebuilder:validation:Enum=Apply;Parse
	// +optional
	Phase string `json:"phase,omitempty"`

	// PID is the configuration persistent identity that failed.
	// +optional
	PID string `json:"pid,omitempty"`
}

// WorkloadRef names the workload that runs this client extension. The workload
// is deployed alongside the ClientExtension by the chart rather than embedded
// here, so it stays an ordinary Deployment, Job or CronJob that Helm and Argo CD
// can reconcile like any other.
//
// Embedding a pod template instead would put corev1.PodSpec into this CRD, and
// the generated schema for it is 8,000 lines -- 97% of the file, and 3.6 times
// over the 262144-byte limit on the last-applied-configuration annotation, which
// makes the CRD impossible to install with a client-side kubectl apply.
//
// The workload always lives in the ClientExtension's own namespace: a reference
// across namespaces could not be resolved without granting this operator read
// access to workloads everywhere.
type WorkloadRef struct {
	// +kubebuilder:default=apps/v1
	// +kubebuilder:validation:Enum=apps/v1;batch/v1
	// +optional
	APIVersion string `json:"apiVersion,omitempty"`

	// +kubebuilder:validation:Enum=CronJob;Deployment;Job
	// +kubebuilder:validation:Required
	Kind string `json:"kind"`

	// +kubebuilder:validation:Required
	Name string `json:"name"`
}

// +kubebuilder:object:root=true
// +kubebuilder:printcolumn:JSONPath=`.spec.liferayEnvironmentRef.namespace`,name="DXP-Namespace",type=string
// +kubebuilder:printcolumn:JSONPath=`.spec.virtualInstanceId`,name="Virtual-Instance",type=string
// +kubebuilder:printcolumn:JSONPath=`.spec.workloadRef.kind`,name="Workload",type=string
// +kubebuilder:printcolumn:JSONPath=`.status.conditions[?(@.type=="Delivered")].status`,name="Delivered",type=string
// +kubebuilder:printcolumn:JSONPath=`.status.conditions[?(@.type=="ConfigurationAccepted")].status`,name="Config-Accepted",type=string
// +kubebuilder:printcolumn:JSONPath=`.status.conditions[?(@.type=="Provisioned")].status`,name="Provisioned",type=string
// +kubebuilder:printcolumn:JSONPath=`.status.phase`,name="Phase",type=string
// +kubebuilder:printcolumn:JSONPath=`.metadata.creationTimestamp`,name="Age",type=date
// +kubebuilder:printcolumn:JSONPath=`.spec.serviceId`,name="Service-ID",priority=1,type=string
// +kubebuilder:resource:shortName=cx
// +kubebuilder:subresource:status
type ClientExtension struct {
	metav1.ObjectMeta `json:"metadata,omitempty"`
	metav1.TypeMeta   `json:",inline"`

	Spec   ClientExtensionSpec   `json:"spec,omitempty"`
	Status ClientExtensionStatus `json:"status,omitempty"`
}

// +kubebuilder:object:root=true
type ClientExtensionList struct {
	metav1.ListMeta `json:"metadata,omitempty"`
	metav1.TypeMeta `json:",inline"`

	Items []ClientExtension `json:"items"`
}

type ClientExtensionSpec struct {
	// ClientExtensionYAML is the expanded client-extension.yaml document. The
	// operator translates it into the OSGi configuration payload. Globs and
	// frontendTokenDefinitionJSON references must already be resolved by the
	// build, because the operator cannot read the built assets.
	// +optional
	ClientExtensionYAML string `json:"clientExtensionYaml,omitempty"`

	// Configs are pre-translated configuration payloads, used when a build
	// emits JSON directly instead of expanded YAML. Mutually exclusive with
	// ClientExtensionYAML.
	// +optional
	Configs []string `json:"configs,omitempty"`

	// Domain is the host a client extension is reached at. One name serves
	// both callers: a browser resolves it through the ingress, and Liferay
	// resolves the same name to the Service inside the cluster. It becomes the
	// ext.lxc.liferay.com/mainDomain annotation, which Liferay turns into
	// .serviceAddress and baseURL.
	// +optional
	Domain string `json:"domain,omitempty"`

	// +kubebuilder:validation:Required
	LiferayEnvironmentRef LiferayEnvironmentRef `json:"liferayEnvironmentRef"`

	// ProjectName seeds projectId, webContextPath and baseURL. It defaults to
	// ServiceID and must match what the build used.
	// +optional
	ProjectName string `json:"projectName,omitempty"`

	// ServiceID becomes the ext.lxc.liferay.com/serviceId label, which is what
	// determines the name of the ext-init ConfigMap Liferay writes back.
	// +kubebuilder:validation:Required
	ServiceID string `json:"serviceId"`

	// VirtualInstanceID is the company web ID, such as liferay.com.
	// +kubebuilder:validation:Required
	VirtualInstanceID string `json:"virtualInstanceId"`

	// WorkloadRef is the workload this client extension runs as. Omit it
	// entirely for a configuration-only client extension.
	// +optional
	WorkloadRef *WorkloadRef `json:"workloadRef,omitempty"`
}

type ClientExtensionStatus struct {
	// +listMapKey=type
	// +listType=map
	// +optional
	Conditions []metav1.Condition `json:"conditions,omitempty"`

	// ExtInitSecretName is the Secret the operator mirrors Liferay's ext-init
	// ConfigMap into. A Secret is used because that payload carries plaintext
	// OAuth2 client secrets.
	// +optional
	ExtInitSecretName string `json:"extInitSecretName,omitempty"`

	// +optional
	AppliedConfigurationPIDs []string `json:"appliedConfigurationPids,omitempty"`

	// ConfigurationErrors are the payload entries the virtual instance
	// refused. They come from Liferay, which is the only component that knows.
	// +optional
	ConfigurationErrors []ConfigurationError `json:"configurationErrors,omitempty"`

	// +optional
	ExtProvisionConfigMapNames []string `json:"extProvisionConfigMapNames,omitempty"`

	// +optional
	ObservedGeneration int64 `json:"observedGeneration,omitempty"`

	// +kubebuilder:validation:Enum=Degraded;Pending;Ready
	// +optional
	Phase string `json:"phase,omitempty"`

	// WorkloadIssues are the specific problems found on the referenced
	// workload. Knowing which mount or variable is missing is the point of
	// validating rather than injecting.
	// +optional
	WorkloadIssues []string `json:"workloadIssues,omitempty"`

	// +optional
	WorkloadName string `json:"workloadName,omitempty"`
}
