#!/usr/bin/env bash

#
# The image's entrypoint sources this script, so the shell options it sets
# stay inside a subshell rather than leak into the entrypoint.
#

function main {
	(
		set -o errexit
		set -o nounset
		set -o pipefail

		local liferay_home_mount=$(stat --format=%m "${LIFERAY_HOME}")
		local overlay_names

		read -a overlay_names -r <<< "${LIFERAY_OVERLAY_NAMES:-}"

		local overlay_name

		for overlay_name in "${overlay_names[@]}"
		do
			local overlay_root="${LIFERAY_OVERLAY_DIR}/${overlay_name}"
			local source_file

			while IFS= read -d "" -r source_file
			do
				local relative_path=$(echo "${source_file}" | sed --expression "s|^${overlay_root}/||")

				if [[ "${relative_path}" == osgi/client-extensions/* ]] ||
				   [[ "${relative_path}" == osgi/configs/* ]] ||
				   [[ "${relative_path}" == osgi/modules/* ]] ||
				   [[ "${relative_path}" == osgi/war/* ]] ||
				   [[ "${relative_path}" == patching/* ]]
				then
					continue
				fi

				local target_file="${LIFERAY_HOME}/${relative_path}"

				local existing_path="${target_file}"

				while [ ! -e "${existing_path}" ]
				do
					existing_path=$(dirname "${existing_path}")
				done

				if [ "$(stat --format=%m "${existing_path}")" != "${liferay_home_mount}" ]
				then
					echo "[LIFERAY] Skipping ${source_file} because ${target_file} is on a volume, where a copy would outlive the overlay."

					continue
				fi

				mkdir --parents "$(dirname "${target_file}")"

				cp \
					--no-dereference \
					--preserve=mode,timestamps \
					"${source_file}" \
					"${target_file}"

				echo "[LIFERAY] Copied ${source_file} to ${target_file}."
			done < <(find "${overlay_root}" -type f -print0 | sort --zero-terminated)
		done
	)
}

main