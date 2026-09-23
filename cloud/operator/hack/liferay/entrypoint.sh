#!/bin/bash

# Applies the overlay that the init container fetched from object storage, then
# starts Tomcat. This is the same shape Liferay Cloud uses: the image is
# immutable and everything environment specific arrives as an overlay.

set -o errexit
set -o nounset
set -o pipefail

LIFERAY_HOME="${LIFERAY_HOME:-/opt/liferay}"

OVERLAY_DIR="${OVERLAY_DIR:-/mnt/liferay/files}"

if [ -d "${OVERLAY_DIR}" ]
then
	echo "Applying overlay from ${OVERLAY_DIR}"

	find "${OVERLAY_DIR}" -type f -printf '  %P\n'

	cp --recursive "${OVERLAY_DIR}/." "${LIFERAY_HOME}/"
else
	echo "No overlay found at ${OVERLAY_DIR}"
fi

mkdir --parents "${LIFERAY_HOME}/data" "${LIFERAY_HOME}/logs"

exec "${LIFERAY_HOME}/tomcat/bin/catalina.sh" run
