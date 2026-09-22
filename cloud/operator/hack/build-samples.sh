#!/bin/bash

# Builds a container image for every sample client extension from the zip its
# Gradle build produced, then loads them into the k3d cluster.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

BUILD_DIR="${BUILD_DIR:-/tmp/cx-spike-images}"

function main {
	_pull_base_images
	_build_samples
	_import_samples
}

function _build_samples {
	log_step "Building sample images"

	rm --force --recursive "${BUILD_DIR}"
	mkdir --parents "${BUILD_DIR}"

	local sample

	for sample in $(samples)
	do
		local context="${BUILD_DIR}/${sample}"

		mkdir --parents "${context}"

		unzip -q -o "${SAMPLES_DIR}/${sample}/dist/${sample}.zip" -d "${context}"

		if ! docker build --quiet --tag "${sample}:${IMAGE_TAG}" "${context}" > /dev/null
		then
			echo "failed to build ${sample}" >&2

			return 1
		fi

		echo "built ${sample}:${IMAGE_TAG}"
	done
}

function _import_samples {
	log_step "Importing sample images into ${CLUSTER_NAME}"

	local images=()
	local sample

	for sample in $(samples)
	do
		images+=("${sample}:${IMAGE_TAG}")
	done

	k3d image import --cluster "${CLUSTER_NAME}" "${images[@]}"
}

function _pull_base_images {
	log_step "Pulling client extension base images"

	local image

	for image in \
		liferay/batch:latest \
		liferay/caddy:latest \
		liferay/jar-runner:latest \
		liferay/node-runner:latest \
		liferay/noop:latest
	do
		docker pull --quiet "${image}" > /dev/null &
	done

	wait

	log "Base images ready"
}

main "${@}"