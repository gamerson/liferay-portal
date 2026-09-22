#!/bin/bash

# Deletes the k3d cluster created by bootstrap.sh.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

k3d cluster delete "${CLUSTER_NAME}"