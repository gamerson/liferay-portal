#!/bin/bash

# Creates a k3d cluster, builds and loads the operator, installs the CRDs, and
# brings up Liferay DXP carrying the rebuilt portal modules.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

OPERATOR_DIR="$(cd "${HACK_DIR}/.." && pwd)"

CLOUD_DIR="$(cd "${OPERATOR_DIR}/.." && pwd)"

function main {
	_create_cluster
	_build_images
	_import_images
	_install_crds
	_install_operator
	_install_liferay

	log_step "Bootstrap complete"

	kube get nodes
	kube get pods --all-namespaces
}

function _build_images {
	log_step "Building the operator"

	docker build \
		--file "${OPERATOR_DIR}/Dockerfile" \
		--tag "${OPERATOR_IMAGE}" \
		"${OPERATOR_DIR}"
}

function _create_cluster {
	if k3d cluster list "${CLUSTER_NAME}" > /dev/null 2>&1
	then
		log "Cluster ${CLUSTER_NAME} already exists"

		return
	fi

	log_step "Creating k3d cluster ${CLUSTER_NAME}"

	mkdir --parents "${PORTAL_MODULES_DIR}"

	# The node mounts the staged modules so the Liferay pod's init container can
	# copy a freshly built bundle in without rebuilding an image.

	k3d cluster create "${CLUSTER_NAME}" \
		--agents 1 \
		--port "8080:80@loadbalancer" \
		--volume "${PORTAL_MODULES_DIR}:/liferay-modules@all" \
		--wait
}

function _import_images {
	log_step "Importing images into ${CLUSTER_NAME}"

	k3d image import \
		--cluster "${CLUSTER_NAME}" \
		"${OPERATOR_IMAGE}"
}

function _install_crds {
	log_step "Installing CRDs"

	kube apply --server-side --force-conflicts \
		--filename "${CLOUD_DIR}/helm/dxp-operator/crds/"

	kube wait --for condition=established --timeout 60s \
		crd/clientextensions.cx.liferay.com \
		crd/liferayenvironments.licensing.liferay.com
}

# _install_liferay brings up a published Liferay DXP image carrying the portal
# modules built from this checkout, copied in by an init container from a volume
# the node mounts.
function _install_liferay {
	log_step "Installing Liferay DXP in ${LIFERAY_NAMESPACE}"

	"${HACK_DIR}/stage-modules.sh"

	kube create namespace "${LIFERAY_NAMESPACE}" --dry-run=client --output yaml | kube apply --filename -
	kube create namespace "${CX_NAMESPACE_SPLIT}" --dry-run=client --output yaml | kube apply --filename -
	kube create namespace "${CX_NAMESPACE_DENIED}" --dry-run=client --output yaml | kube apply --filename -

	kube --namespace "${LIFERAY_NAMESPACE}" create secret generic liferay-activation \
		--dry-run=client --from-literal=activationCode=spike --output yaml | kube apply --filename -

	_render manifests/mariadb.yaml | kube apply --filename -
	_render manifests/liferay-dxp.yaml | kube apply --filename -
	_render manifests/liferay-environment.yaml | kube apply --filename -

	kube --namespace "${LIFERAY_NAMESPACE}" rollout status deployment/mariadb --timeout 300s

	log "Waiting for Liferay to boot. A first boot against an empty database takes several minutes."

	kube --namespace "${LIFERAY_NAMESPACE}" rollout status deployment/liferay --timeout 1800s
}

function _install_operator {
	log_step "Installing the DXP operator"

	kube create namespace "${OPERATOR_NAMESPACE}" --dry-run=client --output yaml | kube apply --filename -

	helm_cx upgrade --install dxp-operator "${CLOUD_DIR}/helm/dxp-operator" \
		--namespace "${OPERATOR_NAMESPACE}" \
		--set "fullnameOverride=dxp-operator" \
		--set "image.pullPolicy=Never" \
		--set "image.repository=${OPERATOR_IMAGE%:*}" \
		--set "image.tag=${OPERATOR_IMAGE##*:}" \
		--set "provisioning.baseURL=http://127.0.0.1:1" \
		--wait \
		--timeout 180s

	kube --namespace "${OPERATOR_NAMESPACE}" rollout status deployment/dxp-operator --timeout 180s
}

function _render {
	sed \
		--expression "s|__CX_NAMESPACE_SPLIT__|${CX_NAMESPACE_SPLIT}|g" \
		--expression "s|__LIFERAY_IMAGE__|${LIFERAY_IMAGE}|g" \
		--expression "s|__LIFERAY_NAMESPACE__|${LIFERAY_NAMESPACE}|g" \
		--expression "s|__MARIADB_IMAGE__|${MARIADB_IMAGE}|g" \
		--expression "s|__MINIO_IMAGE__|${MINIO_IMAGE}|g" \
		--expression "s|__VIRTUAL_INSTANCE_ID__|${VIRTUAL_INSTANCE_ID}|g" \
		"${HACK_DIR}/${1}"
}

main "${@}"
