#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

function main {
	_create_registry
	_create_cluster
	_import_dxp_image
}

function _create_cluster {
	if k3d cluster list "${CLUSTER_NAME}" &> /dev/null
	then
		log "Cluster ${CLUSTER_NAME} already exists."

		return
	fi

	log "Creating cluster ${CLUSTER_NAME} on ${K3S_IMAGE}."

	k3d cluster create \
		--image "${K3S_IMAGE}" \
		--registry-use "${OCI_REGISTRY_HOST}" \
		--wait \
		"${CLUSTER_NAME}"
}

function _create_registry {
	if k3d registry list "${OCI_REGISTRY_NAME}" &> /dev/null
	then
		log "Registry ${OCI_REGISTRY_NAME} already exists."

		return
	fi

	log "Creating registry ${OCI_REGISTRY_HOST}."

	k3d registry create --port "${OCI_REGISTRY_PORT}" "${OCI_REGISTRY_NAME}"
}

function _import_dxp_image {
	if docker exec "k3d-${CLUSTER_NAME}-server-0" crictl images | grep --quiet "${DXP_IMAGE_TAG}"
	then
		log "${DXP_IMAGE_REPOSITORY}:${DXP_IMAGE_TAG} is already in the cluster."

		return
	fi

	log "Importing ${DXP_IMAGE_REPOSITORY}:${DXP_IMAGE_TAG} into the cluster."

	k3d image import --cluster "${CLUSTER_NAME}" "${DXP_IMAGE_REPOSITORY}:${DXP_IMAGE_TAG}"
}

main