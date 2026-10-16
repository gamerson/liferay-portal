#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

#
# publish_overlay.sh <name> <environment> [build | none]
#
# Publishes the sample workspace as an overlay, as a CI pipeline would, and
# records the digest pinned reference under <name> for deploy_liferay.sh. The
# environment picks the configs/<environment> directory. The build stamps the
# demo module, and "none" leaves the module out.
#

function main {
	local build=${3:-v1}
	local environment=${2:?usage: publish_overlay.sh <name> <environment> [build | none]}
	local name=${1:?usage: publish_overlay.sh <name> <environment> [build | none]}

	local arguments=(
		"--console=plain"
		"--project-dir=${POC_DIR}/workspace"
		"-Pliferay.workspace.environment=${environment}"
		"publishOverlay"
	)

	if [ "${build}" == "none" ]
	then
		arguments+=("-Pliferay.workspace.modules.dir=none")
	else
		arguments+=("-Pacme.overlay.demo.build=${build}")
	fi

	if [ "${environment}" == "hotfix" ]
	then
		_download_hotfix
	fi

	log "Publishing ${name} from configs/${environment} with module build ${build}."

	"${REPO_DIR}/gradlew" "${arguments[@]}"

	mkdir --parents "${STATE_DIR}"

	cp "${POC_DIR}/workspace/build/overlay/overlay.json" "${STATE_DIR}/${name}.json"

	jq --raw-output .reference "${STATE_DIR}/${name}.json" > "${STATE_DIR}/${name}.reference"

	log "${name} is $(cat "${STATE_DIR}/${name}.reference")."
}

function _download_hotfix {
	local hotfix_file="${POC_DIR}/workspace/configs/hotfix/patching/$(basename "${HOTFIX_URL}")"

	if [ -f "${hotfix_file}" ]
	then
		return
	fi

	log "Downloading ${HOTFIX_URL}."

	curl \
		--fail \
		--location \
		--output "${hotfix_file}" \
		--silent \
		"${HOTFIX_URL}"
}

main "${@}"