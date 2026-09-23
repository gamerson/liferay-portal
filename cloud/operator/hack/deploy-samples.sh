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

CXEXPAND="${BUILD_DIR:-/tmp/cx-spike-images}/cxexpand"

PUBLIC_DOMAIN_SUFFIX="${PUBLIC_DOMAIN_SUFFIX:-localtest.me}"

# Browsers reach the cluster through the port k3d publishes the load balancer
# on, and an Origin header carries any non default port. Both the asset URL the
# portal hands the browser and the origin the asset host allows have to include
# it, so the port belongs in the domain rather than alongside it.
PUBLIC_DOMAIN_PORT="${PUBLIC_DOMAIN_PORT:-8080}"

function main {
	local cx_namespace=${1}
	local liferay_namespace=${2}

	shift 2

	local requested=("${@}")

	if [ ${#requested[@]} -eq 0 ]
	then
		mapfile -t requested < <(samples)
	fi

	_build_cxexpand

	kube create namespace "${cx_namespace}" --dry-run=client --output yaml | kube apply --filename -

	log_step "Deploying ${#requested[@]} client extensions into ${cx_namespace} (Liferay in ${liferay_namespace})"

	local sample

	for sample in "${requested[@]}"
	do
		_deploy_one "${sample}" "${cx_namespace}" "${liferay_namespace}"
	done
}

function _build_cxexpand {
	if [ -x "${CXEXPAND}" ]
	then
		return
	fi

	mkdir --parents "$(dirname "${CXEXPAND}")"

	(cd "${HACK_DIR}/../resources" && go build -o "${CXEXPAND}" ./cmd/cxexpand)
}

function _deploy_one {
	local sample=${1}
	local cx_namespace=${2}
	local liferay_namespace=${3}

	local expanded="${BUILD_DIR:-/tmp/cx-spike-images}/${sample}.expanded.yaml"

	"${CXEXPAND}" "${SAMPLES_DIR}/${sample}" > "${expanded}"

	local kind
	kind=$(sample_kind "${sample}")

	local port
	port=$(sample_port "${sample}")

	local -a arguments=(
		--create-namespace
		--namespace "${cx_namespace}"
		--set "clientExtension.domains.public=${sample}.${PUBLIC_DOMAIN_SUFFIX}:${PUBLIC_DOMAIN_PORT}"
		--set "clientExtension.serviceId=${sample}"
		--set "clientExtension.virtualInstanceId=${VIRTUAL_INSTANCE_ID}"
		--set "fullnameOverride=${sample}"
		--set "image.pullPolicy=Never"
		--set "image.repository=${sample}"
		--set "image.tag=${IMAGE_TAG}"
		--set "workload.containerPort=${port}"
		--set "workload.kind=${kind}"
		--set-file "clientExtension.clientExtensionYaml=${expanded}"
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

	# A microservice is called by Liferay rather than fetched by a browser, so
	# it gets a Service and a cluster-internal address. The operator then
	# splits the payload into a public and an internal ext-provision ConfigMap.
	if _is_microservice "${sample}"
	then
		arguments+=(
			--set "clientExtension.domains.internal=auto"
			--set "service.enabled=true"
			--set "service.port=${port}"
			--set "service.targetPort=${port}"
		)
	else
		# A frontend client extension is fetched by the browser, so it needs a
		# route of its own. Its assets are then a cross origin request from the
		# portal page, which is why the server hosting them has to know the
		# virtual instance's domains.
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
	fi

	helm_cx upgrade --install "${sample}" "${CHART_DIR}" "${arguments[@]}" > /dev/null

	echo "deployed ${sample} (${kind})"
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