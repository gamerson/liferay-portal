// Package cx reconciles ClientExtension resources: it delivers the
// configuration payload into Liferay's namespace, waits for Liferay to write
// back the OAuth2 credentials, mirrors what the workload needs across a
// namespace boundary, and owns the workload itself.
package cx

import (
	"fmt"
	"strings"

	cxv1alpha1 "github.com/liferay/liferay-portal/cloud/operator/api/cx/v1alpha1"
)

// Labels and annotations understood by Liferay's portal-k8s-agent.
const (
	AnnotationDomains    = "ext.lxc.liferay.com/domains"
	AnnotationMainDomain = "ext.lxc.liferay.com/mainDomain"

	LabelMetadataType    = "lxc.liferay.com/metadataType"
	LabelProjectName     = "ext.lxc.liferay.com/projectName"
	LabelServiceID       = "ext.lxc.liferay.com/serviceId"
	LabelVirtualInstance = "dxp.lxc.liferay.com/virtualInstanceId"

	MetadataTypeDXP          = "dxp"
	MetadataTypeExtInit      = "ext-init"
	MetadataTypeExtProvision = "ext-provision"
)

// Labels and annotations owned by this operator.
const (
	AnnotationDeletionStarted = "cx.liferay.com/deletion-started"
	AnnotationSource          = "cx.liferay.com/source"

	LabelMirror         = "cx.liferay.com/mirror"
	LabelOwnerName      = "cx.liferay.com/owner-name"
	LabelOwnerNamespace = "cx.liferay.com/owner-namespace"

	Finalizer = "cx.liferay.com/unprovision"
)

// Paths and environment the client extension runtime expects.
const (
	EnvRoutesClientExtension = "LIFERAY_ROUTES_CLIENT_EXTENSION"
	EnvRoutesDXP             = "LIFERAY_ROUTES_DXP"

	MountPathExtInit = "/etc/liferay/lxc/ext-init-metadata"
	MountPathDXP     = "/etc/liferay/lxc/dxp-metadata"

	VolumeExtInit = "lxc-ext-init-metadata"
	VolumeDXP     = "lxc-dxp-metadata"
)

const dxpMetadataSuffix = "-lxc-dxp-metadata"

// DXPMetadataName is the ConfigMap Liferay publishes per virtual instance. The
// name is derived from the company web ID by CompanyConfigMapUtil.
func DXPMetadataName(virtualInstanceID string) string {
	return virtualInstanceID + dxpMetadataSuffix
}

// VirtualInstanceOfDXPMetadata reverses DXPMetadataName.
func VirtualInstanceOfDXPMetadata(name string) (string, bool) {
	if !strings.HasSuffix(name, dxpMetadataSuffix) {
		return "", false
	}

	return strings.TrimSuffix(name, dxpMetadataSuffix), true
}

// ExtInitName is the ConfigMap Liferay writes the OAuth2 credentials into. The
// name is computed by BaseConfigurationFactory from the serviceId label and the
// company web ID. The operator never relies on it for lookup -- it watches by
// label instead -- but it is used to name the mirrored Secret.
func ExtInitName(serviceID string, virtualInstanceID string) string {
	return fmt.Sprintf("%s-%s-lxc-ext-init-metadata", serviceID, virtualInstanceID)
}

// ExtInitSecretName is the Secret the ext-init payload is mirrored into. A
// Secret is used because that payload carries plaintext OAuth2 client secrets.
func ExtInitSecretName(clientExtension *cxv1alpha1.ClientExtension) string {
	return truncate(clientExtension.Spec.ServiceID+"-lxc-ext-init", 253)
}

// ExtProvisionName is the ConfigMap the operator publishes into the Liferay
// namespace. There is one per client extension: the domain it is annotated
// with resolves to the same place for a browser and for Liferay, so a second
// one addressed differently would serve no purpose.
func ExtProvisionName(clientExtension *cxv1alpha1.ClientExtension) string {
	return truncate(
		fmt.Sprintf(
			"%s-%s-lxc-ext-provision-metadata",
			clientExtension.Spec.ServiceID, clientExtension.Spec.VirtualInstanceID,
		),
		253,
	)
}

// LiferayNamespace resolves which namespace Liferay runs in for a client
// extension. An empty reference namespace means same-namespace deployment.
func LiferayNamespace(clientExtension *cxv1alpha1.ClientExtension) string {
	if clientExtension.Spec.LiferayEnvironmentRef.Namespace != "" {
		return clientExtension.Spec.LiferayEnvironmentRef.Namespace
	}

	return clientExtension.Namespace
}

// ProjectName resolves the project name used to seed projectId, webContextPath
// and baseURL. It must match what the build used.
func ProjectName(clientExtension *cxv1alpha1.ClientExtension) string {
	if clientExtension.Spec.ProjectName != "" {
		return clientExtension.Spec.ProjectName
	}

	return clientExtension.Spec.ServiceID
}

func truncate(value string, limit int) string {
	if len(value) <= limit {
		return value
	}

	return strings.TrimRight(value[:limit], "-.")
}
