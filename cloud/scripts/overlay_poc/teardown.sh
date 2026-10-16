#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

function main {
	k3d cluster delete "${CLUSTER_NAME}"

	k3d registry delete "k3d-${OCI_REGISTRY_NAME}" 2> /dev/null || true
}

main