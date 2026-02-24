#!/usr/bin/env bash

set -eux

BUCKET="liferay-cloud-native-bootstrap-nonprd"
PACKAGE="liferay-aws-bootstrap"

# 1. Query the GCS API for objects in the specific 'folder'
# We fetch the name and updated time, sorted by updated time descending
API_URL="https://storage.googleapis.com/storage/v1/b/${BUCKET}/o?prefix=${PACKAGE}/&projection=noAcl"

echo "Fetching metadata from GCS API..."

# 2. Use curl to get the object list and jq to find the latest 'updated' file
LATEST_JSON=$(curl -s "${API_URL}")
LATEST_PATH=$(echo "${LATEST_JSON}" | jq -r '.items | sort_by(.updated) | last | .name')

echo $LATEST_JSON > latest.json
echo $LATEST_PATH > latest.path

if [ "$LATEST_PATH" == "null" ]; then
    echo "Error: Could not find any files in gs://${BUCKET}/${PACKAGE}/"
    exit 1
fi


FILE_NAME=$(basename "${LATEST_PATH}")
echo $FILE_NAME

BASE_URL="https://cdn.liferay.sh/"
VERSION_URL="${BASE_URL}/${LATEST_PATH}"

echo "Latest version identified: ${FILE_NAME}"
echo "Downloading via CDN: ${VERSION_URL}"
#
## 3. Download the actual file from the CDN
curl -L "${VERSION_URL}" -o "${FILE_NAME}"
#
echo "Bootstrap complete: ${FILE_NAME} is ready."