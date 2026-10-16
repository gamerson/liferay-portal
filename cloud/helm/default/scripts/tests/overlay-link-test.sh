#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

function main {
	local fail=0
	local pass=0
	local script

	script=$(cd "$(dirname "${0}")/.." && pwd)/overlay-link.sh

	_run_test "${script}" _test_keeps_files_and_links_the_overlay_does_not_manage
	_run_test "${script}" _test_links_each_file_into_the_matching_liferay_directory
	_run_test "${script}" _test_removes_links_to_files_no_overlay_provides
	_run_test "${script}" _test_replaces_a_link_from_an_earlier_overlay_with_a_later_one

	echo ""
	echo "Results: ${pass} passed, ${fail} failed."

	if [ "${fail}" -eq 0 ]
	then
		return 0
	fi

	return 1
}

function _overlay_file {
	mkdir --parents "$(dirname "${1}")"

	echo "${2:-content}" > "${1}"
}

function _run_link {
	local script=${1}
	local work_dir=${2}

	shift 2

	LIFERAY_OVERLAY_DIR="${work_dir}/overlays" LIFERAY_OVERLAY_NAMES="${*}" LIFERAY_OVERLAY_TARGET_DIR="${work_dir}/pvc" bash "${script}" > /dev/null
}

function _run_test {
	local script="${1}"
	local test_function="${2}"

	local description

	description=$(echo "${test_function}" | sed "s/^_test_//; s/_/ /g")

	local exit_code=0
	local work_dir

	work_dir=$(mktemp -d)

	"${test_function}" "${script}" "${work_dir}" || exit_code="${?}"

	rm --force --recursive "${work_dir}"

	if [ "${exit_code}" -eq 0 ]
	then
		echo "PASS: ${description}."

		((++pass))
	else
		echo "FAIL: ${description}."

		((++fail))
	fi
}

function _test_keeps_files_and_links_the_overlay_does_not_manage {
	local script=${1}
	local work_dir=${2}

	_overlay_file "${work_dir}/overlays/a/osgi/modules/shared.jar"
	_overlay_file "${work_dir}/pvc/osgi/modules/shared.jar" customer
	_overlay_file "${work_dir}/elsewhere/foreign.jar"

	ln --symbolic "${work_dir}/elsewhere/foreign.jar" "${work_dir}/pvc/osgi/modules/foreign.jar"

	_run_link "${script}" "${work_dir}" a

	[ ! -L "${work_dir}/pvc/osgi/modules/shared.jar" ] &&
	[[ $(cat "${work_dir}/pvc/osgi/modules/shared.jar") == customer ]] &&
	[[ $(readlink "${work_dir}/pvc/osgi/modules/foreign.jar") == "${work_dir}/elsewhere/foreign.jar" ]]
}

function _test_links_each_file_into_the_matching_liferay_directory {
	local script=${1}
	local work_dir=${2}

	_overlay_file "${work_dir}/overlays/a/osgi/configs/com.acme.Config.config"
	_overlay_file "${work_dir}/overlays/a/osgi/modules/com.acme.jar"
	_overlay_file "${work_dir}/overlays/a/tomcat/lib/driver.jar"

	_run_link "${script}" "${work_dir}" a

	[[ $(readlink "${work_dir}/pvc/osgi/configs/com.acme.Config.config") == "${work_dir}/overlays/a/osgi/configs/com.acme.Config.config" ]] &&
	[[ $(readlink "${work_dir}/pvc/osgi/modules/com.acme.jar") == "${work_dir}/overlays/a/osgi/modules/com.acme.jar" ]] &&
	[ ! -e "${work_dir}/pvc/tomcat" ]
}

function _test_removes_links_to_files_no_overlay_provides {
	local script=${1}
	local work_dir=${2}

	_overlay_file "${work_dir}/overlays/a/osgi/modules/kept.jar"
	_overlay_file "${work_dir}/overlays/a/osgi/modules/removed.jar"
	_overlay_file "${work_dir}/overlays/b/osgi/configs/removed.config"

	_run_link "${script}" "${work_dir}" a b

	rm "${work_dir}/overlays/a/osgi/modules/removed.jar"

	_run_link "${script}" "${work_dir}" a

	[ -L "${work_dir}/pvc/osgi/modules/kept.jar" ] &&
	[ ! -L "${work_dir}/pvc/osgi/modules/removed.jar" ] &&
	[ ! -L "${work_dir}/pvc/osgi/configs/removed.config" ]
}

function _test_replaces_a_link_from_an_earlier_overlay_with_a_later_one {
	local script=${1}
	local work_dir=${2}

	_overlay_file "${work_dir}/overlays/a/osgi/modules/com.acme.jar"
	_overlay_file "${work_dir}/overlays/b/osgi/modules/com.acme.jar"

	_run_link "${script}" "${work_dir}" a b

	[[ $(readlink "${work_dir}/pvc/osgi/modules/com.acme.jar") == "${work_dir}/overlays/b/osgi/modules/com.acme.jar" ]]
}

main "${@}"