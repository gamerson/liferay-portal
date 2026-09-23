#!/bin/bash

# Collects the state of every client extension in the cluster into a Markdown
# status report.
#
# report.sh [output-file]

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

REPORT_FILE="${1:-${HACK_DIR}/../STATUS_REPORT.md}"

function main {
	log "Writing ${REPORT_FILE}"

	{
		_header
		_conformance
		_scenarios
		_handshake
		_notes
	} > "${REPORT_FILE}"

	log "Report written"
}

function _conformance {
	echo "## Configuration Translation Conformance"
	echo
	echo "The Go translator is compared against the payload the Gradle task produced for every sample."
	echo

	echo '```'

	(cd "${HACK_DIR}/../resources" && go test ./internal/cxconfig/... -run TestTranslateMatchesGradleOutput -v 2>&1 |
		grep --extended-regexp '^(=== RUN|--- (PASS|FAIL)|ok|FAIL|PASS)' |
		tail -5) || true

	echo '```'
	echo

	local passed

	passed=$(cd "${HACK_DIR}/../resources" && go test ./internal/cxconfig/... -run TestTranslateMatchesGradleOutput -v 2>&1 |
		grep --count -- '--- PASS' || true)

	echo "Sample projects translated byte-equivalent to the Gradle output: **$((passed - 1)) of $(samples | wc -l)**."
	echo
}

function _handshake {
	echo "## Handshake Artifacts"
	echo
	echo "Objects produced by one client extension, end to end."
	echo

	echo '```'

	echo "# 1. The chart renders a ClientExtension"
	kube --namespace "${LIFERAY_NAMESPACE}" get clientextension liferay-sample-etc-spring-boot \
		--output 'custom-columns=NAME:.metadata.name,SERVICE-ID:.spec.serviceId,VI:.spec.virtualInstanceId,INTERNAL:.spec.domains.internal,PUBLIC:.spec.domains.public' 2> /dev/null || true

	echo
	echo "# 2. The operator publishes ext-provision ConfigMaps, split by addressing bucket"
	kube --namespace "${LIFERAY_NAMESPACE}" get configmap \
		--selector "lxc.liferay.com/metadataType=ext-provision,ext.lxc.liferay.com/serviceId=liferay-sample-etc-spring-boot" \
		--output 'custom-columns=NAME:.metadata.name,MAIN-DOMAIN:.metadata.annotations.ext\.lxc\.liferay\.com/mainDomain' 2> /dev/null || true

	echo
	echo "# 3. Liferay writes back ext-init with the OAuth2 credentials"
	kube --namespace "${LIFERAY_NAMESPACE}" get configmap \
		--selector "lxc.liferay.com/metadataType=ext-init,ext.lxc.liferay.com/serviceId=liferay-sample-etc-spring-boot" \
		--output 'custom-columns=NAME:.metadata.name,KEYS:.data' 2> /dev/null | cut -c 1-110 || true

	echo
	echo "# 4. The operator mirrors it into a Secret, never a ConfigMap"
	kube --namespace "${LIFERAY_NAMESPACE}" get secret liferay-sample-etc-spring-boot-lxc-ext-init \
		--output 'custom-columns=NAME:.metadata.name,TYPE:.type,KEYS:.data' 2> /dev/null | cut -c 1-110 || true

	echo
	echo "# 5. The virtual instance reports back on the payload it was given"
	kube --namespace "${LIFERAY_NAMESPACE}" get configmap \
		--selector "lxc.liferay.com/metadataType=ext-status" \
		--output 'custom-columns=NAME:.metadata.name,ACCEPTED:.data.accepted,ERRORS:.data.errorCount' 2> /dev/null | head -4 || true

	echo
	echo "# 6. A rejected payload names the stage that failed"
	kube --namespace "${LIFERAY_NAMESPACE}" get clientextension broken-payload-demo \
		--output jsonpath='{.status.conditions[?(@.type=="ConfigurationAccepted")].message}{"\n"}' 2> /dev/null || true

	echo
	echo "# 7. The workload mounts both, and the operator injected them"
	kube --namespace "${LIFERAY_NAMESPACE}" get deployment liferay-sample-etc-spring-boot \
		--output jsonpath='{range .spec.template.spec.volumes[*]}{.name}{" -> "}{.configMap.name}{.secret.secretName}{"\n"}{end}' 2> /dev/null || true

	echo
	echo "# 8. A shared virtual instance mirror, owned by every client extension using it"
	kube --namespace "${CX_NAMESPACE_SPLIT}" get configmap "${VIRTUAL_INSTANCE_ID}-lxc-dxp-metadata" \
		--output jsonpath='{.metadata.name}{" owners="}{range .metadata.ownerReferences[*]}{.name}{","}{end}' 2> /dev/null | cut -c 1-200 || true

	echo
	echo '```'
	echo
}

function _header {
	echo "# Client Extension Operator: Status Report"
	echo
	echo "Generated $(date --utc --iso-8601=seconds) against k3d cluster \`${CLUSTER_NAME}\`."
	echo
	echo "| Component | Value |"
	echo "|---|---|"
	echo "| Cluster | $(kube version --output json 2> /dev/null | python3 -c 'import json,sys; print(json.load(sys.stdin)["serverVersion"]["gitVersion"])' 2> /dev/null || echo unknown) |"
	echo "| Liferay namespace | \`${LIFERAY_NAMESPACE}\` |"
	echo "| Liferay image | \`${LIFERAY_IMAGE}\` plus modules from this checkout |"
	echo "| Operator | \`${OPERATOR_IMAGE}\` in \`${OPERATOR_NAMESPACE}\` |"
	echo "| Split namespace | \`${CX_NAMESPACE_SPLIT}\` |"
	echo "| Unlisted namespace | \`${CX_NAMESPACE_DENIED}\` |"
	echo
}

function _notes {
	echo "## What Is Under Test"
	echo
	echo "Everything in this report runs against a published Liferay DXP image carrying the portal modules built from this checkout. Nothing is simulated: the ext-provision payloads are read by \`portal-k8s-agent\`, the OAuth2 applications are registered by Liferay, and the credentials in every ext-init Secret were issued by it."
	echo
	echo "A workload that is not Ready is therefore a real failure rather than a limit of the harness. The CronJob sample is the exception -- it is created on its declared schedule and does not run inside the test window."
	echo
}

function _scenarios {
	echo "## Scenarios"
	echo

	_scenario_table "Scenario A: Liferay and client extensions in one namespace" "${LIFERAY_NAMESPACE}"
	_scenario_table "Scenario B: client extensions in a separate namespace" "${CX_NAMESPACE_SPLIT}"
	_scenario_table "Scenario C: a namespace Liferay has not consented to" "${CX_NAMESPACE_DENIED}"
}

function _scenario_table {
	local title=${1}
	local namespace=${2}

	echo "### ${title}"
	echo

	kube --namespace "${namespace}" get clientextension --output json 2> /dev/null |
		REPORT_NAMESPACE="${namespace}" python3 "${HACK_DIR}/report_table.py" || echo "_No client extensions in \`${namespace}\`._"

	echo
}

main "${@}"