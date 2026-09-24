#!/bin/bash

# Rebuilds the operator image, reloads it into the cluster and restarts the
# deployment. Used while iterating on the controller.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

OPERATOR_DIR="$(cd "${HACK_DIR}/.." && pwd)"

docker build --quiet --file "${OPERATOR_DIR}/Dockerfile" --tag "${OPERATOR_IMAGE}" "${OPERATOR_DIR}" > /dev/null

k3d image import --cluster "${CLUSTER_NAME}" "${OPERATOR_IMAGE}"

kube --namespace "${OPERATOR_NAMESPACE}" rollout restart deployment/dxp-operator
kube --namespace "${OPERATOR_NAMESPACE}" rollout status deployment/dxp-operator --timeout 180s