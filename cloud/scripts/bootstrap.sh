#!/usr/bin/env bash

set -eux

BUCKET_NAME="liferay-cloud-native-bootstrap-nonprd"
PREFIX="bootstrap/liferay-aws-bootstrap"

API_URL="https://storage.googleapis.com/storage/v1/b/${BUCKET_NAME}/o?prefix=${PREFIX}/&projection=noAcl"

echo "Fetching metadata from GCS API..."

LATEST_JSON=$(curl -s "${API_URL}")
LATEST_PATH=$(echo "${LATEST_JSON}" | jq -r '.items | sort_by(.updated) | last | .name')

echo "$LATEST_JSON" > latest.json
echo "$LATEST_PATH" > latest.path

if [ "$LATEST_PATH" == "null" ]; then
    echo "Error: Could not find any files in gs://${BUCKET_PATH}/${PREFIX}/"
    exit 1
fi

FILE_NAME=$(basename "${LATEST_PATH}")

BASE_URL="https://cdn.liferay.sh"
VERSION_URL="${BASE_URL}/${LATEST_PATH}"

echo "Latest version identified: ${FILE_NAME}"
echo "Downloading via CDN: ${VERSION_URL}"
curl -L "${VERSION_URL}" -o "${FILE_NAME}"
echo "Bootstrap complete: ${FILE_NAME} is ready."