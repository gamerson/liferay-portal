#!/bin/bash

# Assembles the Liferay overlay that MinIO serves: the client extension status
# module, the rebuilt portal-k8s-agent bundles, and the environment specific
# portal configuration.
#
# The overlay carries everything environment specific, so the portal image
# stays immutable. This mirrors how Liferay Cloud delivers an overlay from
# object storage.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

MODULES_DIR="$(cd "${HACK_DIR}/../../../modules" && pwd)"

OVERLAY_DIR="${HACK_DIR}/overlay"

function main {
	_build_modules
	_assemble
}

function _assemble {
	log_step "Assembling the overlay"

	mkdir --parents "${OVERLAY_DIR}/osgi/modules" "${OVERLAY_DIR}/osgi/static"

	cp \
		"${MODULES_DIR}/apps/static/portal-k8s-web/build/libs/com.liferay.portal.k8s.web-1.0.0.jar" \
		"${OVERLAY_DIR}/osgi/modules/"

	cp \
		"${MODULES_DIR}/apps/static/portal-k8s-agent/portal-k8s-agent-api/build/libs/com.liferay.portal.k8s.agent.api-4.0.3.jar" \
		"${OVERLAY_DIR}/osgi/static/com.liferay.portal.k8s.agent.api.jar"

	cp \
		"${MODULES_DIR}/apps/static/portal-k8s-agent/portal-k8s-agent-impl/build/libs/com.liferay.portal.k8s.agent.impl-3.0.55.jar" \
		"${OVERLAY_DIR}/osgi/static/com.liferay.portal.k8s.agent.impl.jar"

	find "${OVERLAY_DIR}" -type f -not -path '*/.git*' -printf '  %P\n'
}

function _build_modules {
	log_step "Building the portal modules"

	# These produce a plain class jar unless the portal snapshot is installed
	# in the local .m2. Run "ant all" at the repository root, then
	# "ant deploy install-portal-snapshot" from portal-impl, first.

	local module

	for module in \
		apps/static/portal-k8s-agent/portal-k8s-agent-api \
		apps/static/portal-k8s-agent/portal-k8s-agent-impl \
		apps/static/portal-k8s-web
	do
		(cd "${MODULES_DIR}/${module}" && "${MODULES_DIR}/../gradlew" --quiet jar)
	done

	if ! unzip -p \
			"${MODULES_DIR}/apps/static/portal-k8s-web/build/libs/com.liferay.portal.k8s.web-1.0.0.jar" \
			META-INF/MANIFEST.MF 2> /dev/null |
			grep --quiet Bundle-SymbolicName
	then
		echo "The modules did not build as OSGi bundles." >&2
		echo "Install the portal snapshot first: ant all, then ant deploy install-portal-snapshot." >&2

		return 1
	fi
}

main "${@}"
