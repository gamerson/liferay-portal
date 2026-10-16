#!/usr/bin/env bash

CLUSTER_NAME="${CLUSTER_NAME:-overlay-poc}"

DXP_IMAGE_REPOSITORY="${DXP_IMAGE_REPOSITORY:-liferay/dxp}"

DXP_IMAGE_TAG="${DXP_IMAGE_TAG:-2026.q3.5}"

HOTFIX_URL="${HOTFIX_URL:-https://releases-cdn.liferay.com/dxp/hotfix/2026.q3.5/liferay-dxp-2026.q3.5-hotfix-3.zip}"

#
# Image volumes need Kubernetes 1.33 or newer, and a containerd that
# implements them.
#

K3S_IMAGE="${K3S_IMAGE:-rancher/k3s:v1.36.4-k3s1}"

KUBE_CONTEXT="k3d-${CLUSTER_NAME}"

NAMESPACE="${NAMESPACE:-liferay}"

#
# A registry named under .localhost resolves to the host on the host and to the
# registry container inside the cluster, so the reference the workspace prints
# is the one the cluster pulls.
#

OCI_REGISTRY_NAME="${OCI_REGISTRY_NAME:-overlay-oci.localhost}"

OCI_REGISTRY_PORT="${OCI_REGISTRY_PORT:-5112}"

OCI_REGISTRY_HOST="k3d-${OCI_REGISTRY_NAME}:${OCI_REGISTRY_PORT}"

POC_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

REPO_DIR=$(cd "${POC_DIR}/../../.." && pwd)

STATE_DIR="${POC_DIR}/.state"

function kube {
	kubectl --context "${KUBE_CONTEXT}" "${@}"
}

function log {
	echo "[overlay-poc] ${*}"
}