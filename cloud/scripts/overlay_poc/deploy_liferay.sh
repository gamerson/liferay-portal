#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

#
# deploy_liferay.sh [name[:hotfix] ...]
#
# Installs or upgrades the liferay-default chart with one overlay artifact per
# name, in order, from the references publish_overlay.sh recorded. A name
# suffixed with :hotfix provides the hotfix. With no names, the overlay is
# removed. Waits for the rollout to finish.
#

function main {
	mkdir --parents "${STATE_DIR}"

	local values_file="${STATE_DIR}/overlay-values.yaml"

	_write_values "${values_file}" "${@}"

	log "Deploying with $(tr '\n' ' ' < "${values_file}")"

	helm upgrade \
		--create-namespace \
		--install \
		--kube-context "${KUBE_CONTEXT}" \
		--namespace "${NAMESPACE}" \
		--values "${POC_DIR}/values.yaml" \
		--values "${values_file}" \
		liferay \
		"${REPO_DIR}/cloud/helm/default"

	kube \
		--namespace "${NAMESPACE}" \
		rollout \
		status \
		--timeout 30m \
		statefulset/liferay-default
}

function _write_values {
	local values_file=${1}

	shift

	echo "overlay:" > "${values_file}"

	if [[ "${#}" -eq 0 ]]
	then
		echo "    artifacts: []" >> "${values_file}"

		return
	fi

	echo "    artifacts:" >> "${values_file}"

	local artifact

	for artifact in "${@}"
	do
		local name=${artifact%%:*}

		echo "        -   name: ${name}" >> "${values_file}"
		echo "            reference: $(cat "${STATE_DIR}/${name}.reference")" >> "${values_file}"

		if [[ "${artifact}" == *:hotfix ]]
		then
			echo "            hotfix: true" >> "${values_file}"
		fi
	done
}

main "${@}"