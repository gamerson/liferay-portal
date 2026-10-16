#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

function main {
	local fail=0
	local pass=0
	local script

	script=$(cd "$(dirname "${0}")/.." && pwd)/overlay-files.sh

	_run_test "${script}" _test_copies_files_into_liferay_home_by_their_overlay_path
	_run_test "${script}" _test_does_not_change_the_shell_that_sources_it
	_run_test "${script}" _test_leaves_linked_directories_and_patching_to_other_steps
	_run_test "${script}" _test_lets_a_later_overlay_replace_an_earlier_file

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

function _run_files {
	local script=${1}
	local work_dir=${2}

	shift 2

	mkdir --parents "${work_dir}/home"

	LIFERAY_HOME="${work_dir}/home" LIFERAY_OVERLAY_DIR="${work_dir}/overlays" LIFERAY_OVERLAY_NAMES="${*}" bash -c "source \"${script}\"" > /dev/null
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

function _test_copies_files_into_liferay_home_by_their_overlay_path {
	local script=${1}
	local work_dir=${2}

	_overlay_file "${work_dir}/overlays/a/tomcat/conf/catalina.properties" overlay
	_overlay_file "${work_dir}/overlays/a/tomcat/lib/ext/driver.jar" driver

	_run_files "${script}" "${work_dir}" a

	[[ $(cat "${work_dir}/home/tomcat/conf/catalina.properties") == overlay ]] &&
	[[ $(cat "${work_dir}/home/tomcat/lib/ext/driver.jar") == driver ]]
}

function _test_does_not_change_the_shell_that_sources_it {
	local script=${1}
	local work_dir=${2}

	mkdir --parents "${work_dir}/home"

	local options

	options=$(LIFERAY_HOME="${work_dir}/home" LIFERAY_OVERLAY_DIR="${work_dir}/overlays" LIFERAY_OVERLAY_NAMES=missing bash -c "source \"${script}\" > /dev/null 2>&1; echo \"\${-}\"")

	[[ "${options}" != *e* ]] && [[ "${options}" != *u* ]]
}

function _test_leaves_linked_directories_and_patching_to_other_steps {
	local script=${1}
	local work_dir=${2}

	_overlay_file "${work_dir}/overlays/a/osgi/configs/com.acme.Config.config"
	_overlay_file "${work_dir}/overlays/a/osgi/modules/com.acme.jar"
	_overlay_file "${work_dir}/overlays/a/patching/liferay-dxp-hotfix.zip"

	_run_files "${script}" "${work_dir}" a

	[ ! -e "${work_dir}/home/osgi" ] && [ ! -e "${work_dir}/home/patching" ]
}

function _test_lets_a_later_overlay_replace_an_earlier_file {
	local script=${1}
	local work_dir=${2}

	_overlay_file "${work_dir}/overlays/a/tomcat/conf/catalina.properties" a
	_overlay_file "${work_dir}/overlays/b/tomcat/conf/catalina.properties" b

	_run_files "${script}" "${work_dir}" a b

	[[ $(cat "${work_dir}/home/tomcat/conf/catalina.properties") == b ]]
}

main "${@}"