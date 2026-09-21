#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

#
# Builds an offline activation bundle the operator accepts, so the control
# panel flow can be exercised without a real one from the provisioning server.
# The operator only base64 decodes the license, and it skips the add-on
# extraction entirely when the manifest entitles none, so an empty "add-ons"
# list plus the directory entry is the smallest bundle that activates.
#

function main {
	local output_file="${1:-${_SCRIPT_DIR}/test-activation-bundle.zip}"

	_work_dir=$(mktemp --directory)

	trap 'rm --force --recursive "${_work_dir:-}"' EXIT

	mkdir --parents "${_work_dir}/add-ons"

	touch "${_work_dir}/add-ons/.keep"

	local license_xml

	license_xml=$(_license_xml | base64 --wrap 0)

	cat > "${_work_dir}/manifest.json" <<EOF
{
	"add-ons": [],
	"licenseXML": "${license_xml}",
	"maxClusterNodes": 3
}
EOF

	rm --force "${output_file}"

	(cd "${_work_dir}" && zip --quiet --recurse-paths "${output_file}" .)

	echo "Wrote ${output_file}"
	echo
	unzip -l "${output_file}"
}

function _license_xml {
	cat <<'EOF'
<?xml version="1.0"?>
<license>
	<description>Offline activation PoC license</description>
	<product-entry>
		<name>Liferay DXP</name>
	</product-entry>
</license>
EOF
}

_work_dir=""

_SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

main "${@}"