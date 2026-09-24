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

CHART_DIR="$(cd "${HACK_DIR}/../../helm/client-extension" && pwd)"

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

	# The payload is taken verbatim from the build artifact rather than
	# retranslated. The zip already carries the JSON the Gradle task produced,
	# with globs resolved and frontend token definitions inlined, so what is
	# deployed is byte for byte what was built.
	local payload="${BUILD_DIR:-/tmp/cx-spike-images}/${sample}.config.json"

	_extract_payload "${sample}" "${payload}"

	local kind
	kind=$(sample_kind "${sample}")

	local port
	port=$(sample_port "${sample}")

	local -a arguments=(
		--create-namespace
		--namespace "${cx_namespace}"
		--set "clientExtension.domain=${sample}.${PUBLIC_DOMAIN_SUFFIX}"
		--set "clientExtension.serviceId=${sample}"
		--set "clientExtension.virtualInstanceId=${VIRTUAL_INSTANCE_ID}"
		--set "fullnameOverride=${sample}"
		--set "image.pullPolicy=Never"
		--set "image.repository=${sample}"
		--set "image.tag=${IMAGE_TAG}"
		--set "workload.containerPort=${port}"
		--set "workload.kind=${kind}"
		--set-file "clientExtension.configs[0]=${payload}"
	)

	if [ "${cx_namespace}" != "${liferay_namespace}" ]
	then
		arguments+=(--set "clientExtension.liferayEnvironment.namespace=${liferay_namespace}")
	fi

	if [ "${kind}" == "CronJob" ]
	then
		arguments+=(--set "workload.schedule=$(sample_schedule "${sample}")")
	fi

	# The batch runner needs to be told which OAuth2 application to use. The
	# client-extension.yaml carries that cross reference, but the generated
	# payload drops it because batch has no configuration PID of its own.
	local batch_erc
	batch_erc=$(_oauth_reference "${sample}")

	if [ -n "${batch_erc}" ]
	then
		arguments+=(--set "workload.env.LIFERAY_BATCH_OAUTH_APP_ERC=${batch_erc}")
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
		--set "service.targetPort=${port}"
	)

	helm_cx upgrade --install "${sample}" "${CHART_DIR}" "${arguments[@]}" > /dev/null

	echo "deployed ${sample} (${kind})"
}

# _extract_payload pulls the generated configuration out of the artifact. Every
# sample ships exactly one.
function _extract_payload {
	local sample=${1}
	local destination=${2}

	mkdir --parents "$(dirname "${destination}")"

	local entry

	entry=$(unzip -Z1 "${SAMPLES_DIR}/${sample}/dist/${sample}.zip" \
		'*client-extension-config.json' 2> /dev/null | head -1)

	if [ -z "${entry}" ]
	then
		echo "no client-extension-config.json in ${sample}.zip" >&2

		return 1
	fi

	unzip -p "${SAMPLES_DIR}/${sample}/dist/${sample}.zip" "${entry}" > "${destination}"
}

function _oauth_reference {
	local sample=${1}

	python3 -c '
import sys, yaml
document = yaml.safe_load(open(sys.argv[1])) or {}
for entry in document.values():
	if isinstance(entry, dict) and entry.get("type") in ("batch", "siteInitializer"):
		print(entry.get("oAuthApplicationHeadlessServer", ""))
		break
' "${SAMPLES_DIR}/${sample}/client-extension.yaml" 2> /dev/null || echo ""
}

function _is_microservice {
	local sample=${1}

	unzip -p "${SAMPLES_DIR}/${sample}/dist/${sample}.zip" Dockerfile 2> /dev/null |
		grep --quiet --extended-regexp 'FROM liferay/(jar-runner|node-runner)'
}

main "${@}"