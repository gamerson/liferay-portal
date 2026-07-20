#!/usr/bin/env bash
#
# Shared configuration and helpers for the local operator test scripts.
# Every value can be overridden from the environment, e.g.:
#
#     CLUSTER_NAME=my-cluster ./e2e.sh
#
# shellcheck disable=SC2034  # variables are consumed by the sourcing scripts

set -euo pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OPERATOR_DIR="$(cd "${HACK_DIR}/.." && pwd)"
REPO_ROOT="$(cd "${OPERATOR_DIR}/../.." && pwd)"
HELM_CHART="${REPO_ROOT}/cloud/helm/operator"
MANIFESTS_DIR="${HACK_DIR}/manifests"

CLUSTER_NAME="${CLUSTER_NAME:-liferay-operator-test}"
KUBE_CONTEXT="${KUBE_CONTEXT:-k3d-${CLUSTER_NAME}}"

IMAGE_REPOSITORY="${IMAGE_REPOSITORY:-liferay/liferay-cloud-operator}"
IMAGE_TAG="${IMAGE_TAG:-dev}"
IMAGE="${IMAGE_REPOSITORY}:${IMAGE_TAG}"

RELEASE="${RELEASE:-liferay-operator}"
OPERATOR_NAMESPACE="${OPERATOR_NAMESPACE:-liferay-system}"
TEST_NAMESPACE="${TEST_NAMESPACE:-acme-prod}"

log() {
	printf '\033[1;34m==>\033[0m %s\n' "$*"
}

warn() {
	printf '\033[1;33m warning:\033[0m %s\n' "$*" >&2
}

fail() {
	printf '\033[1;31m error:\033[0m %s\n' "$*" >&2
	exit 1
}

require_cmd() {
	for cmd in "$@"; do
		command -v "${cmd}" >/dev/null 2>&1 ||
			fail "required command not found on PATH: ${cmd}"
	done
}

kc() {
	kubectl --context "${KUBE_CONTEXT}" "$@"
}
