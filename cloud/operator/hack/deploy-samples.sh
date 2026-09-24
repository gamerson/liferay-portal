#!/bin/bash

# Installs the client extension chart once per sample into a target namespace.
#
# deploy-samples.sh <cx-namespace> <liferay-namespace> [sample ...]
#
# When the two namespaces match, the client extension is deployed alongside
# Liferay and no mirroring is needed. When they differ, the operator mirrors
# the virtual instance metadata and the ext-init credentials across the
# boundary.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

CHART_VERSION="${CHART_VERSION:-0.1.0}"

PUBLIC_DOMAIN_SUFFIX="${PUBLIC_DOMAIN_SUFFIX:-localtest.me}"

function main {
	local cx_namespace=${1}
	local liferay_namespace=${2}

	shift 2

	local requested=("${@}")

	if [ ${#requested[@]} -eq 0 ]
	then
		mapfile -t requested < <(samples)
	fi

	kube create namespace "${cx_namespace}" --dry-run=client --output yaml | kube apply --filename -

	log_step "Deploying ${#requested[@]} client extensions into ${cx_namespace} (Liferay in ${liferay_namespace})"

	local sample

	for sample in "${requested[@]}"
	do
		_deploy_one "${sample}" "${cx_namespace}" "${liferay_namespace}"
	done
}

function _deploy_one {
	local sample=${1}
	local cx_namespace=${2}
	local liferay_namespace=${3}

	# Publish exactly as CI would, then install from the registry exactly as
	# Argo CD would: the chart already carries the payload, the workload shape,
	# the base image and the pinned artifact, so only the environment binding is
	# passed here.
	"${HACK_DIR}/package-cx-chart.sh" "${sample}" "${CHART_VERSION}" > /dev/null

	local -a arguments=(
		--create-namespace
		--namespace "${cx_namespace}"
		--plain-http
		--set "clientExtension.domain=${sample}.${PUBLIC_DOMAIN_SUFFIX}"
		--set "clientExtension.virtualInstanceId=${VIRTUAL_INSTANCE_ID}"
		--version "${CHART_VERSION}"
	)

	if [ "${cx_namespace}" != "${liferay_namespace}" ]
	then
		arguments+=(--set "clientExtension.liferayEnvironment.namespace=${liferay_namespace}")
	fi

	# Every client extension gets a route of its own. A frontend one is fetched
	# by the browser; a microservice is reached by the browser too whenever it
	# exposes a user agent OAuth2 application, which the browser calls directly.
	arguments+=(
		--set "ingress.className=traefik"
		--set "ingress.enabled=true"
		--set "ingress.hosts[0].host=${sample}.${PUBLIC_DOMAIN_SUFFIX}"
		--set "ingress.hosts[0].paths[0].path=/"
		--set "ingress.hosts[0].paths[0].pathType=Prefix"
		--set "service.enabled=true"
		--set "service.port=80"
	)

	helm_cx upgrade --install "${sample}" "oci://${OCI_PUSH_HOST}/charts/${sample}" \
		"${arguments[@]}" > /dev/null

	echo "deployed ${sample} ($(sample_kind "${sample}"))"
}

main "${@}"