#!/bin/bash

# Records every package a Liferay image exports, so a master-built bundle can
# have its import floors lowered to what the image actually provides.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

EXPORTS_FILE="${EXPORTS_FILE:-${PORTAL_MODULES_DIR}/image-exports.json}"

function main {
	log_step "Scanning ${LIFERAY_IMAGE} for exported packages"

	mkdir --parents "$(dirname "${EXPORTS_FILE}")"

	# Emit every manifest verbatim with a separator. Continuation lines are
	# unfolded on this side, where the parsing is reliable.

	docker run --rm --entrypoint sh "${LIFERAY_IMAGE}" -c '
		for dir in \
			/opt/liferay/osgi/core \
			/opt/liferay/osgi/marketplace \
			/opt/liferay/osgi/modules \
			/opt/liferay/osgi/portal \
			/opt/liferay/osgi/static \
			/opt/liferay/tomcat/webapps/ROOT/WEB-INF/shielded-container-lib
		do
			[ -d "${dir}" ] || continue

			find "${dir}" -name "*.jar" | while read -r jar
			do
				echo "===JAR=== ${jar}"

				unzip -p "${jar}" META-INF/MANIFEST.MF 2> /dev/null || true
			done
		done
	' > "${EXPORTS_FILE}.raw"

	python3 "${HACK_DIR}/parse-image-exports.py" \
		"${EXPORTS_FILE}.raw" "${EXPORTS_FILE}"

	rm --force "${EXPORTS_FILE}.raw"
}

main "${@}"