#!/bin/bash

# Deletes the k3d cluster and OCI registry created by bootstrap.sh.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

k3d cluster delete "${CLUSTER_NAME}"

k3d registry delete "${OCI_REGISTRY_NAME}" 2> /dev/null || true