package cx

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	runtime "k8s.io/apimachinery/pkg/runtime"
	client "sigs.k8s.io/controller-runtime/pkg/client"
	apiutil "sigs.k8s.io/controller-runtime/pkg/client/apiutil"
)

// appendNonControllerOwner adds an owner reference without claiming to be the
// controller, so several client extensions can share one mirrored ConfigMap.
// The garbage collector prunes a reference when its owner is deleted and
// collects the object once the last owner is gone.
func appendNonControllerOwner(
	owner client.Object, object client.Object, scheme *runtime.Scheme,
) error {
	groupVersionKind, error := apiutil.GVKForObject(owner, scheme)

	if error != nil {
		return error
	}

	reference := metav1.OwnerReference{
		APIVersion:         groupVersionKind.GroupVersion().String(),
		BlockOwnerDeletion: ptr(false),
		Controller:         ptr(false),
		Kind:               groupVersionKind.Kind,
		Name:               owner.GetName(),
		UID:                owner.GetUID(),
	}

	references := object.GetOwnerReferences()

	for index := range references {
		if references[index].UID == reference.UID {
			references[index] = reference

			object.SetOwnerReferences(references)

			return nil
		}
	}

	object.SetOwnerReferences(append(references, reference))

	return nil
}

func ptr[T any](value T) *T {
	return &value
}
