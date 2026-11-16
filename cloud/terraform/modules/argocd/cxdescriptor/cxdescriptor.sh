#!/bin/bash

function check_registry {
	local reference="${1}"

	if [ -z "${CXDESCRIPTOR_REGISTRIES}" ]
	then
		return 0
	fi

	local registry

	for registry in ${CXDESCRIPTOR_REGISTRIES}
	do
		if [[ "${reference}" == "${registry%/}/"* ]]
		then
			return 0
		fi
	done

	echo "${reference} is not in an allowed client extension registry: ${CXDESCRIPTOR_REGISTRIES}" >&2

	return 1
}

function main {
	local reference="${4#cxdescriptor://}"

	if ! check_registry "${reference}"
	then
		return 1
	fi

	local image_file

	image_file=$(mktemp)

	trap "rm -f ${image_file}" EXIT

	# The repo server mounts the optional argocd-cxdescriptor-registry-credentials
	# Secret here. crane reads its config.json for private registries, and reads
	# anonymously when the Secret does not exist and the directory is empty.

	export DOCKER_CONFIG="/cxdescriptor-docker-config"

	if ! "${HELM_PLUGIN_DIR}/bin/crane" export -- "${reference}" "${image_file}"
	then
		return 1
	fi

	tar --extract --file "${image_file}" --to-stdout client-extension-descriptor.json
}

main "${@}"