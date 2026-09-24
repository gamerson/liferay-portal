#!/bin/bash

# Builds the three portal modules this branch changes and stages them where the
# k3d node can see them. The cluster mounts that directory, and the Liferay pod's
# init container copies from it -- so a rebuilt module reaches the portal by
# restarting the pod, with no image rebuild.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

DIST_DIR="$(cd "${HACK_DIR}/../../../tools/sdk/dist" && pwd)"

MODULES_DIR="$(cd "${HACK_DIR}/../../../modules" && pwd)"

function main {
	_build
	_stage
	_adapt

	log "Restart the Liferay pod to pick these up:"
	log "  kubectl --namespace ${LIFERAY_NAMESPACE} delete pod --selector app=liferay"
}

# _adapt lowers each import floor to the version the target image exports. A
# bundle built against this branch otherwise requires packages newer than a
# published image provides, and a bundle in osgi/static that cannot resolve
# stops the portal from booting.
function _adapt {
	log_step "Adapting the bundles to ${LIFERAY_IMAGE}"

	if [ ! -f "${PORTAL_MODULES_DIR}/image-exports.json" ]
	then
		"${HACK_DIR}/scan-image-exports.sh"
	fi

	python3 "${HACK_DIR}/adapt-bundles.py" \
		--exports "${PORTAL_MODULES_DIR}/image-exports.json" \
		$(find "${PORTAL_MODULES_DIR}" -name '*.jar' | sort)
}

function _build {
	log_step "Building the portal modules"

	local module

	for module in \
		apps/static/portal-k8s-agent/portal-k8s-agent-api \
		apps/static/portal-k8s-agent/portal-k8s-agent-impl \
		apps/static/portal-k8s-web
	do
		log "Building ${module}"

		(cd "${MODULES_DIR}/${module}" && "${MODULES_DIR}/../gradlew" --quiet jar -x pmdMain -x pmdTest)
	done
}

function _stage {
	log_step "Staging modules in ${PORTAL_MODULES_DIR}"

	# Clear the contents, never the directory itself. The k3d nodes bind mount
	# it, and a directory that is removed and recreated leaves each node holding
	# the old inode: the mount goes silently empty, and the init container that
	# copies the modules in crashes on a glob that matches nothing.
	mkdir --parents "${PORTAL_MODULES_DIR}"

	find "${PORTAL_MODULES_DIR}" -mindepth 1 -type f -name '*.jar' -delete

	mkdir --parents "${PORTAL_MODULES_DIR}/osgi/modules" "${PORTAL_MODULES_DIR}/osgi/static"

	cp "${DIST_DIR}/com.liferay.portal.k8s.web-1.0.0.jar" \
		"${PORTAL_MODULES_DIR}/osgi/modules/"

	# The agent bundles keep the names the image ships them under, so copying
	# them over the bundle replaces the stock ones exactly.

	cp "${DIST_DIR}/com.liferay.portal.k8s.agent.api-4.0.3.jar" \
		"${PORTAL_MODULES_DIR}/osgi/static/com.liferay.portal.k8s.agent.api.jar"

	cp "${DIST_DIR}/com.liferay.portal.k8s.agent.impl-3.0.55.jar" \
		"${PORTAL_MODULES_DIR}/osgi/static/com.liferay.portal.k8s.agent.impl.jar"

	local jar

	for jar in $(find "${PORTAL_MODULES_DIR}" -name '*.jar' | sort)
	do
		printf '  %-52s %s\n' \
			"$(unzip -p "${jar}" META-INF/MANIFEST.MF | grep Bundle-SymbolicName | tr -d '\r' | cut -d' ' -f2)" \
			"$(unzip -p "${jar}" META-INF/MANIFEST.MF | grep Bundle-Version | tr -d '\r' | cut -d' ' -f2)"
	done
}

main "${@}"