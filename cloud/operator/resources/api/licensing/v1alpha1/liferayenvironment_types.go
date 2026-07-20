package v1alpha1

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func init() {
	SchemeBuilder.Register(&LiferayEnvironment{}, &LiferayEnvironmentList{})
}

// LiferayEnvironment is the Schema for the licensing agent. One resource
// represents one logical Liferay cluster confined to a single namespace.
//
// +kubebuilder:object:root=true
// +kubebuilder:subresource:status
// +kubebuilder:resource:shortName=lenv
// +kubebuilder:printcolumn:name="Phase",type=string,JSONPath=`.status.phase`
// +kubebuilder:printcolumn:name="Max Nodes",type=integer,JSONPath=`.status.license.maxClusterNodes`
// +kubebuilder:printcolumn:name="Effective",type=integer,JSONPath=`.status.effectiveReplicas`
// +kubebuilder:printcolumn:name="Valid Until",type=string,JSONPath=`.status.license.validUntil`
// +kubebuilder:printcolumn:name="Age",type=date,JSONPath=`.metadata.creationTimestamp`
type LiferayEnvironment struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   LiferayEnvironmentSpec   `json:"spec,omitempty"`
	Status LiferayEnvironmentStatus `json:"status,omitempty"`
}

// LiferayEnvironmentList contains a list of LiferayEnvironment.
//
// +kubebuilder:object:root=true
type LiferayEnvironmentList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`

	Items []LiferayEnvironment `json:"items"`
}

// LiferayEnvironmentSpec defines the desired state of a licensed environment.
type LiferayEnvironmentSpec struct {
	// ActivationCodeSecretRef points to the Secret holding the one-time
	// activation code delivered out of band from the Liferay license portal.
	//
	// +kubebuilder:validation:Required
	ActivationCodeSecretRef SecretKeyRef `json:"activationCodeSecretRef"`

	// DesiredReplicas is the operator's intended cluster size. The agent
	// enforces min(desiredReplicas, status.license.maxClusterNodes).
	//
	// +kubebuilder:validation:Minimum=1
	// +kubebuilder:default=1
	DesiredReplicas int32 `json:"desiredReplicas,omitempty"`

	// DxpVersion overrides the DXP version reported to provisioning. When
	// empty the agent derives it from the workload's container image tag.
	//
	// +optional
	DxpVersion string `json:"dxpVersion,omitempty"`

	// EnvironmentName is an optional human-readable identifier for the
	// environment, forwarded to provisioning during activation.
	//
	// +optional
	EnvironmentName string `json:"environmentName,omitempty"`

	// HeartbeatInterval is how often the agent re-checks entitlements.
	//
	// +kubebuilder:default="10m"
	HeartbeatInterval metav1.Duration `json:"heartbeatInterval,omitempty"`

	// WorkloadRef identifies the Liferay workload whose replica count the
	// agent clamps to the licensed node ceiling.
	//
	// +kubebuilder:validation:Required
	WorkloadRef WorkloadRef `json:"workloadRef"`
}

// SecretKeyRef selects a single key from a Secret in the same namespace.
type SecretKeyRef struct {
	// +kubebuilder:validation:Required
	Name string `json:"name"`

	// +kubebuilder:validation:Required
	Key string `json:"key"`
}

// WorkloadRef references the Liferay cluster workload.
type WorkloadRef struct {
	// +kubebuilder:validation:Enum=StatefulSet;Deployment
	// +kubebuilder:default=StatefulSet
	Kind string `json:"kind,omitempty"`

	// +kubebuilder:validation:Required
	Name string `json:"name"`
}

// LiferayEnvironmentStatus captures the observed state.
type LiferayEnvironmentStatus struct {
	// ActivatedAt is set once activation with provisioning succeeds.
	//
	// +optional
	ActivatedAt *metav1.Time `json:"activatedAt,omitempty"`

	// Apps tracks the Marketplace add-ons the agent has materialized.
	//
	// +optional
	// +listType=map
	// +listMapKey=virtualEntryId
	Apps []AppStatus `json:"apps,omitempty"`

	// Conditions follows the standard Kubernetes condition convention
	// (Activated, LicenseValid, ReplicasClamped, ProvisioningReachable).
	//
	// +optional
	// +listType=map
	// +listMapKey=type
	Conditions []metav1.Condition `json:"conditions,omitempty"`

	// EffectiveReplicas is the enforced replica count actually applied to
	// the workload: min(spec.desiredReplicas, license.maxClusterNodes).
	//
	// +optional
	EffectiveReplicas int32 `json:"effectiveReplicas,omitempty"`

	// EnvironmentId is the namespace UID reported to provisioning.
	//
	// +optional
	EnvironmentId string `json:"environmentId,omitempty"`

	// License summarizes the current entitlement.
	//
	// +optional
	License LicenseStatus `json:"license,omitempty"`

	// Phase is a coarse lifecycle summary.
	//
	// +kubebuilder:validation:Enum=Pending;Activating;Ready;Degraded;Suspended
	// +optional
	Phase string `json:"phase,omitempty"`
}

// LicenseStatus summarizes the entitlement returned by provisioning.
type LicenseStatus struct {
	// Checksum is the sha256 of the current license XML.
	//
	// +optional
	Checksum string `json:"checksum,omitempty"`

	// LastVerified is the last time entitlements were successfully fetched.
	//
	// +optional
	LastVerified *metav1.Time `json:"lastVerified,omitempty"`

	// MaxClusterNodes is the licensed ceiling on logical Liferay nodes.
	//
	// +optional
	MaxClusterNodes int32 `json:"maxClusterNodes,omitempty"`

	// ValidUntil is the license expiry.
	//
	// +optional
	ValidUntil *metav1.Time `json:"validUntil,omitempty"`
}

// AppStatus tracks one Marketplace add-on.
type AppStatus struct {
	// Checksum is the sha256 of the downloaded lpkg.
	//
	// +optional
	Checksum string `json:"checksum,omitempty"`

	// Name is the human-readable app name from entitlements.
	//
	// +optional
	Name string `json:"name,omitempty"`

	// State is the materialization state of the add-on.
	//
	// +kubebuilder:validation:Enum=Pending;Downloaded;Deployed;Failed
	// +optional
	State string `json:"state,omitempty"`

	// VirtualEntryId is the Marketplace artifact ID parsed from the
	// download link.
	VirtualEntryId string `json:"virtualEntryId"`
}
