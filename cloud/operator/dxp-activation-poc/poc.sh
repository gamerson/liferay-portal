#!/bin/bash

set -o errexit
set -o nounset
set -o pipefail

function main {
	cd "$(dirname "${BASH_SOURCE[0]}")"

	_BUILD_DIR="$(pwd)/build"
	_CLUSTER_NAME="dxp-activation-poc"
	_IMAGE_TAG="lcd-53829"
	_LIFERAY_NAMESPACE="liferay-dxp-poc"
	_REPOSITORY_DIR=$(git rev-parse --show-toplevel)

	_BUNDLES_DIR="${_REPOSITORY_DIR}/../bundles"

	local command=${1:-}

	if [ "${command}" == "down" ]
	then
		k3d cluster delete "${_CLUSTER_NAME}"
	elif [ "${command}" == "gogo" ]
	then
		_run_gogo_command "${2}" "${3:-0}"
	elif [ "${command}" == "mock" ]
	then
		_call_mock_provisioning "${@:2}"
	elif [ "${command}" == "up" ]
	then
		_deploy_modules
		_build_images
		_create_cluster
		_install_operator
		_install_mock_provisioning
		_install_liferay
	else
		echo "Usage: ${0} down | gogo <command> [pod ordinal] | mock <method> <path> [body] | up"

		exit 1
	fi
}

function _build_dxp_image {
	#
	# Modules built from this branch only resolve against the portal built
	# from it, so the image swaps the portal that "ant all" produced into a
	# released DXP image, which keeps the released runtime and entry point
	#

	local tomcat_version=$(awk -F "=" '/^ *app.server.tomcat.version=/ {print $2}' "${_REPOSITORY_DIR}/app.server.properties")

	mkdir --parents "${_BUILD_DIR}/bundle/tomcat/webapps"

	rsync --archive --delete --exclude "state" "${_BUNDLES_DIR}/osgi" "${_BUILD_DIR}/bundle/"
	rsync --archive --delete "${_BUNDLES_DIR}/tomcat-${tomcat_version}/lib" "${_BUILD_DIR}/bundle/tomcat/"
	rsync --archive --delete "${_BUNDLES_DIR}/tomcat-${tomcat_version}/webapps/ROOT" "${_BUILD_DIR}/bundle/tomcat/webapps/"

	docker build \
		--file Dockerfile.dxp \
		--tag "liferay/dxp-environment-poc:${_IMAGE_TAG}" \
		"${_BUILD_DIR}"
}

function _build_go_image {
	local binary=${1}
	local image=${2}
	local package=${3}

	(
		cd "${_REPOSITORY_DIR}/cloud/operator/resources"

		CGO_ENABLED=0 GOOS=linux go build -o "${_BUILD_DIR}/${binary}" "${package}"
	)

	docker build \
		--build-arg "BINARY=${binary}" \
		--file Dockerfile.go \
		--tag "${image}:${_IMAGE_TAG}" \
		"${_BUILD_DIR}"
}

function _build_images {
	_build_go_image manager liferay/liferay-dxp-operator .
	_build_go_image mockprovisioning liferay/mock-provisioning ./cmd/mockprovisioning

	_build_dxp_image
}

function _call_mock_provisioning {
	local body=${3:-}
	local method=${1}
	local path=${2}

	kubectl \
		exec \
		--container liferay-default \
		--namespace "${_LIFERAY_NAMESPACE}" \
		liferay-default-0 \
		-- \
		curl \
			--data "${body}" \
			--request "${method}" \
			--silent \
			"http://mock-provisioning.liferay-system.svc:8080${path}"

	echo
}

function _create_cluster {
	if ! k3d cluster list "${_CLUSTER_NAME}" &> /dev/null
	then
		k3d cluster create "${_CLUSTER_NAME}" --wait
	fi

	kubectl config use-context "k3d-${_CLUSTER_NAME}"

	k3d image import \
		--cluster "${_CLUSTER_NAME}" \
		--mode direct \
		"liferay/dxp-environment-poc:${_IMAGE_TAG}" \
		"liferay/liferay-dxp-operator:${_IMAGE_TAG}" \
		"liferay/mock-provisioning:${_IMAGE_TAG}"
}

function _deploy_modules {
	mkdir --parents "${_BUILD_DIR}"

	local module_dir

	for module_dir in \
		portal-environment/portal-environment-activation-poc \
		portal-environment/portal-environment-api \
		portal-environment/portal-environment-impl \
		portal-k8s-agent/portal-k8s-agent-impl
	do
		(
			cd "${_REPOSITORY_DIR}/modules/apps/static/${module_dir}"

			"${_REPOSITORY_DIR}/gradlew" deploy
		)
	done
}

function _install_liferay {
	kubectl create namespace "${_LIFERAY_NAMESPACE}" --dry-run=client --output yaml | kubectl apply --filename -

	helm upgrade \
		--install \
		--namespace "${_LIFERAY_NAMESPACE}" \
		--values liferay-values.yaml \
		liferay-default \
		"${_REPOSITORY_DIR}/cloud/helm/default"
}

function _install_mock_provisioning {
	kubectl apply --filename mock-provisioning.yaml

	kubectl rollout status deployment/mock-provisioning --namespace liferay-system
}

function _install_operator {
	helm upgrade \
		--create-namespace \
		--install \
		--namespace liferay-system \
		--set debug=true \
		--set heartbeatInterval=30s \
		--set image.repository=liferay/liferay-dxp-operator \
		--set "image.tag=${_IMAGE_TAG}" \
		--set provisioning.baseURL=http://mock-provisioning.liferay-system.svc:8080 \
		--wait \
		liferay-dxp-operator \
		"${_REPOSITORY_DIR}/cloud/helm/dxp-operator"
}

function _run_gogo_command {
	local gogo_command=${1}
	local pod_ordinal=${2}

	kubectl \
		exec \
		--container liferay-default \
		--namespace "${_LIFERAY_NAMESPACE}" \
		"liferay-default-${pod_ordinal}" \
		-- \
		bash -c "(echo '${gogo_command}'; sleep 8) | telnet localhost 11311 2> /dev/null | sed --expression 's/^g! //' | grep --extended-regexp --invert-match '^(Connected|Escape|Trying|Welcome|_+$|$)'"
}

main "${@}"