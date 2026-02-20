#!/bin/bash

set -eux

PROJECT="liferaycloud-research"
LOCATION="us-central1"
REPO="bootstrap-scripts"
PACKAGE="liferay-aws-bootstrap"
BASE_URL="https://artifactregistry.googleapis.com/v1/projects"

VERSION_PATH=$(curl -s "${BASE_URL}/${PROJECT}/locations/${LOCATION}/repositories/${REPO}/packages/${PACKAGE}/versions" \
    | jq -r '.versions | sort_by(.updateTime) | last | .name')

FILE_PATH=$(curl -s "${BASE_URL}/${PROJECT}/locations/${LOCATION}/repositories/${REPO}/files?filter=owner=\"${VERSION_PATH}\"" \
	| jq -r '.files[0].name')

FILE_NAME=$(basename "${FILE_PATH}")

PUBLIC_URL="https://artifactregistry.googleapis.com/download/v1/projects/${PROJECT}/locations/${LOCATION}/repositories/${REPO}/files/${FILE_NAME}:download?alt=media"

echo "Downloading from: ${PUBLIC_URL}"

OUTPUT_FILE_NAME=$(echo "${FILE_NAME}" | cut -d':' -f3)

curl -L "${PUBLIC_URL}" -o "${OUTPUT_FILE_NAME}"

echo "Download complete."