#!/bin/bash

# Runs the whole spike end to end: cluster, operator, simulated Liferay, sample
# images, all three scenarios, then the status report.
#
# run-all.sh [--keep]
#
# Without --keep the cluster is left running so the report can be re-generated.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

function main {
	"${HACK_DIR}/bootstrap.sh"
	"${HACK_DIR}/build-samples.sh"

	log_step "Scenario A: Liferay and client extensions in one namespace"

	"${HACK_DIR}/deploy-samples.sh" "${LIFERAY_NAMESPACE}" "${LIFERAY_NAMESPACE}"

	log_step "Scenario B: client extensions in a separate namespace"

	"${HACK_DIR}/deploy-samples.sh" "${CX_NAMESPACE_SPLIT}" "${LIFERAY_NAMESPACE}"

	log_step "Scenario C: a namespace Liferay has not consented to"

	"${HACK_DIR}/deploy-samples.sh" "${CX_NAMESPACE_DENIED}" "${LIFERAY_NAMESPACE}" \
		liferay-sample-iframe-2

	_wait_for_settle

	"${HACK_DIR}/report.sh"
}

function _wait_for_settle {
	log_step "Waiting for the client extensions to settle"

	local deadline=$((SECONDS + 420))
	local total

	total=$(samples | wc -l)

	while [ "${SECONDS}" -lt "${deadline}" ]
	do
		local provisioned

		provisioned=$(kube --namespace "${LIFERAY_NAMESPACE}" get clientextension \
			--output jsonpath='{range .items[*]}{.status.conditions[?(@.type=="Provisioned")].status}{"\n"}{end}' |
			grep --count True || true)

		if [ "${provisioned}" -ge "${total}" ]
		then
			log "All ${total} client extensions provisioned"

			return
		fi

		log "Provisioned ${provisioned}/${total}"

		sleep 15
	done

	log "Settle window expired; the report records whatever state was reached"
}

main "${@}"