#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

function main {
	local overlay_dir=${LIFERAY_OVERLAY_DIR:?LIFERAY_OVERLAY_DIR must be set}
	local target_dir=${LIFERAY_OVERLAY_TARGET_DIR:?LIFERAY_OVERLAY_TARGET_DIR must be set}

	local linked_dir

	for linked_dir in osgi/client-extensions osgi/configs osgi/modules osgi/war
	do
		mkdir --parents "${target_dir}/${linked_dir}"

		_unlink "${overlay_dir}" "${target_dir}/${linked_dir}"
	done

	local overlay_names

	read -a overlay_names -r <<< "${LIFERAY_OVERLAY_NAMES:-}"

	local overlay_name

	for overlay_name in "${overlay_names[@]}"
	do
		for linked_dir in osgi/client-extensions osgi/configs osgi/modules osgi/war
		do
			_link "${overlay_dir}" "${overlay_dir}/${overlay_name}/${linked_dir}" "${target_dir}/${linked_dir}"
		done
	done
}

function _link {
	local overlay_dir=${1}
	local source_dir=${2}
	local target_dir=${3}

	if [ ! -d "${source_dir}" ]
	then
		return 0
	fi

	local source_file

	while IFS= read -d "" -r source_file
	do
		local target_file="${target_dir}/$(basename "${source_file}")"

		if [ -L "${target_file}" ] && [[ "$(readlink "${target_file}")" == "${overlay_dir}"/* ]]
		then
			_log_json "Replacing \"${target_file}\" with \"${source_file}\" from a later overlay."

			ln \
				--force \
				--no-dereference \
				--symbolic \
				"${source_file}" \
				"${target_file}"
		elif [ -e "${target_file}" ] || [ -L "${target_file}" ]
		then
			_log_json "Skipping \"${source_file}\" because \"${target_file}\" exists and is not managed by an overlay." "WARNING"
		else
			_log_json "Linking \"${target_file}\" to \"${source_file}\"."

			ln --symbolic "${source_file}" "${target_file}"
		fi
	done < <(find "${source_dir}" -maxdepth 1 -mindepth 1 -type f -print0 | sort --zero-terminated)
}

function _log_json {
	local escaped_message

	escaped_message=$(echo "${1}" | sed --expression 's/"/\\"/g')

	local script_name

	script_name=$(basename "${0}")

	local severity="${2:-INFO}"

	local timestamp

	timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

	printf '{"message": "%s", "script": "%s", "severity": "%s", "timestamp": "%s"}\n' "${escaped_message}" "${script_name}" "${severity}" "${timestamp}"
}

function _unlink {
	local overlay_dir=${1}
	local target_dir=${2}

	local link

	while IFS= read -d "" -r link
	do
		_log_json "Removing \"${link}\", which linked to \"$(readlink "${link}")\"."

		rm "${link}"
	done < <(find "${target_dir}" -maxdepth 1 -mindepth 1 -type l -lname "${overlay_dir}/*" -print0)
}

main