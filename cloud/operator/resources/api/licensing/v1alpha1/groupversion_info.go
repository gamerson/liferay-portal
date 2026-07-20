// Package v1alpha1 contains the API types for the licensing.liferay.com group.
//
// +kubebuilder:object:generate=true
// +groupName=licensing.liferay.com
package v1alpha1

import (
	"k8s.io/apimachinery/pkg/runtime/schema"
	"sigs.k8s.io/controller-runtime/pkg/scheme"
)

var (
	// GroupVersion is the group and version used to register these objects.
	GroupVersion = schema.GroupVersion{
		Group:   "licensing.liferay.com",
		Version: "v1alpha1",
	}

	// SchemeBuilder registers the group's types with a Scheme.
	SchemeBuilder = &scheme.Builder{GroupVersion: GroupVersion}

	// AddToScheme adds the group's types to a Scheme.
	AddToScheme = SchemeBuilder.AddToScheme
)
