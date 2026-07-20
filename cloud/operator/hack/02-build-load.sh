#!/usr/bin/env bash
#
# Build the operator image from the Dockerfile and import it into the k3d
# cluster, so the chart can run it with pullPolicy=IfNotPresent (no registry).

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

require_cmd docker k3d

log "Building image ${IMAGE} from ${OPERATOR_DIR}."

# The Dockerfile does `COPY resources .`, so the build context is the operator
# directory (which contains resources/).
docker build \
	--tag "${IMAGE}" \
	--file "${OPERATOR_DIR}/Dockerfile" \
	"${OPERATOR_DIR}"

log "Importing ${IMAGE} into cluster '${CLUSTER_NAME}'."

k3d image import "${IMAGE}" --cluster "${CLUSTER_NAME}"

log "Image loaded."
