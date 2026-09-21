#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

#
# Builds the three bundles the license activation control panel needs and
# collects them in one directory. The PoC cluster mounts that directory into
# the k3d node, and an init container copies it into /opt/liferay/deploy on
# every pod start, so a rebuild plus a pod restart is the whole edit cycle.
#

function main {
	local repo_root

	repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)

	local output_dir="${1:-${repo_root}/cloud/scripts/poc/jars}"

	mkdir --parents "${output_dir}"

	_build "${repo_root}"

	_collect "${repo_root}" "${output_dir}"

	echo
	echo "The bundles are in ${output_dir}:"
	echo

	ls -1 "${output_dir}"
}

function _build {
	local repo_root=${1}

	echo "Building the license activation bundles. The first run installs the"
	echo "portal snapshots and takes several minutes."
	echo

	"${repo_root}/gradlew" \
		--parallel \
		--project-dir "${repo_root}/modules" \
		:apps:license-manager:license-manager-k8s:jar \
		:apps:license-manager:license-manager-web:jar \
		:apps:static:portal-k8s-agent:portal-k8s-agent-api:jar
}

function _collect {
	local repo_root=${1}
	local output_dir=${2}

	rm --force "${output_dir}"/*.jar

	local dist_dir="${repo_root}/tools/sdk/dist"

	local bundle

	for bundle in \
		"com.liferay.license.manager.k8s" \
		"com.liferay.license.manager.web" \
		"com.liferay.portal.k8s.agent.api"
	do
		local jar

		jar=$(find "${dist_dir}" -maxdepth 1 -name "${bundle}-*.jar" | sort | tail -1)

		if [ -z "${jar}" ]
		then
			echo "Unable to find a built jar for ${bundle} in ${dist_dir}." >&2

			exit 1
		fi

		cp --force --verbose "${jar}" "${output_dir}"
	done
}

main "${@}"