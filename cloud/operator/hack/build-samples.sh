#!/bin/bash

# Stages the stock base images the sample client extensions run on.
#
# Nothing client extension specific is built here. Each sample's files are
# published as an OCI artifact by package-cx-chart.sh and mounted into one of
# these images as an image volume, so the only images a cluster needs are the
# Liferay base images themselves. They are imported into k3d up front so the
# scenarios do not depend on Docker Hub rate limits.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

BASE_IMAGES=(
	liferay/batch:latest
	liferay/caddy:latest
	liferay/jar-runner:latest
	liferay/node-runner:latest
	liferay/noop:latest
)

function main {
	_pull_base_images
	_import_base_images
}

function _import_base_images {
	log_step "Importing base images into ${CLUSTER_NAME}"

	k3d image import --cluster "${CLUSTER_NAME}" "${BASE_IMAGES[@]}"
}

function _pull_base_images {
	log_step "Pulling client extension base images"

	local image

	for image in "${BASE_IMAGES[@]}"
	do
		docker pull --quiet "${image}" > /dev/null &
	done

	wait

	log "Base images ready"
}

main "${@}"