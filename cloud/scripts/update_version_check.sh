#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

_SCRIPTS_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

_ROOT_CLOUD_DIR=$(cd "${_SCRIPTS_DIR}/.." && pwd)

function main {
	find "${_ROOT_CLOUD_DIR}" -name "Chart.yaml" -type f | while read -r chart_yaml_file;
	do
		_check_chart_yaml "$(dirname "${chart_yaml_file}")"
	done

	_check_aws_bootstrap
}

function _bump_bootstrap_version {
	local versions_json="${_SCRIPTS_DIR}/versions.json"

	local current_version
	current_version=$(jq -r '."liferay-aws-bootstrap"' "${versions_json}")

	local new_version
	new_version=$(echo "${current_version}" | awk -F. -v OFS=. '{$NF += 1; print}')
	sed --in-place "${blame_line}s/\"${1}\": .*/\"${1}\": \"${new_version}\"/" "${versions_json}"
}


function _bump_chart_yaml_version {
	local helm_chart_yaml="${1}"

	local current_version
	current_version=$(sed --quiet "${blame_line}p" "${helm_chart_yaml}" | awk '{print $2}')

	local new_version
	new_version=$(echo "${current_version}" | awk -F. -v OFS=. '{$NF += 1; print}')

	sed --in-place "${blame_line}s/version: .*/version: ${new_version}/" "${helm_chart_yaml}"
}

function _check_aws_bootstrap {
	local versions_json="${_SCRIPTS_DIR}/versions.json"

	local blame_line
	blame_line=$(grep --extended-regexp --line-number '"liferay-aws-bootstrap": ".*"$' "${versions_json}" | cut --delimiter=':' --fields=1)

	local target_sha
	target_sha=$(git blame -L "${blame_line}","${blame_line}" -- "${versions_json}" | cut -d' ' -f1)

	local aws_bootstrap_sources=(
		"${_ROOT_CLOUD_DIR}/scripts/setup_aws.sh"
		"${_ROOT_CLOUD_DIR}/scripts/versions.tfvars"
		"${_ROOT_CLOUD_DIR}/terraform/aws/eks"
		"${_ROOT_CLOUD_DIR}/terraform/aws/gitops/platform"
		"${_ROOT_CLOUD_DIR}/terraform/aws/gitops/resources"
		"${_ROOT_CLOUD_DIR}/terraform/aws/grafana"
	)

	for source in "${aws_bootstrap_sources[@]}"
	do
		local commit_count
		commit_count=$(git rev-list --count "${target_sha}..HEAD" -- "${source}")

		if [[ "${commit_count}" -gt 0 ]]; then
			git rev-list --oneline "${target_sha}..HEAD" -- "${source}"
			echo "The version in ${versions_json} is outdated. Updating liferay-aws-bootstrap version."
			echo ""

			_bump_bootstrap_version "liferay-aws-bootstrap"

			exit 0
		fi
	done

}

function _check_chart_yaml {
	local helm_dir="${1}"

	local helm_chart_yaml="${helm_dir}/Chart.yaml"

	local blame_line
	blame_line=$(grep --extended-regexp --line-number "^version: .*$" "${helm_chart_yaml}" | cut --delimiter=':' --fields=1)

	local target_sha
	target_sha=$(git blame -L "${blame_line}","${blame_line}" -- "${helm_chart_yaml}" | cut -d' ' -f1)

	local commit_count
	commit_count=$(git rev-list --count "${target_sha}..HEAD" -- "${helm_dir}")

	if [[ "${commit_count}" -gt 0 ]]
	then
		git rev-list --oneline "${target_sha}..HEAD" -- "${helm_dir}"
		echo "The version in ${helm_chart_yaml} is outdated. Updating Chart.yaml version."
		echo ""

		_bump_chart_yaml_version "${helm_chart_yaml}"
	fi
}

main "$@"