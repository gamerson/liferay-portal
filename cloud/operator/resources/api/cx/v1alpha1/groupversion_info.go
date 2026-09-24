// Package v1alpha1 contains the ClientExtension API used to deploy a Liferay
// client extension into a cluster and bind it to one virtual instance.
//
// +kubebuilder:object:generate=true
// +groupName=cx.liferay.com
package v1alpha1

import (
	schema "k8s.io/apimachinery/pkg/runtime/schema"
	scheme "sigs.k8s.io/controller-runtime/pkg/scheme"
)

var (
	// AddToScheme registers every type in this group with a scheme.
	AddToScheme = SchemeBuilder.AddToScheme

	// GroupVersion is the group and version for this API.
	GroupVersion = schema.GroupVersion{Group: "cx.liferay.com", Version: "v1alpha1"}

	// SchemeBuilder collects the types in this group.
	SchemeBuilder = &scheme.Builder{GroupVersion: GroupVersion}
)
