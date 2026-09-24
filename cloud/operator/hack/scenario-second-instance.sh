#!/bin/bash

# Runs the multi virtual instance scenario end to end.
#
# scenario-second-instance.sh [web-id] [cx-namespace]
#
# One Liferay, two virtual instances, and the same four samples deployed twice.
# The point is that both halves of the handshake are keyed by virtual instance:
# the routes travel out to a namespace Liferay does not own, and the credentials
# that come back are the second instance's own, not a copy of the first's.
#
# Assumes bootstrap.sh and build-samples.sh have already run.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

ADMIN_PASSWORD="${ADMIN_PASSWORD:-test}"

SCENARIO_SAMPLES=(
	liferay-sample-custom-element-1
	liferay-sample-custom-element-2
	liferay-sample-etc-node
	liferay-sample-etc-spring-boot
)

function main {
	local web_id=${1:-virtual1.localtest.me}
	local cx_namespace=${2:-liferay-vi2}

	"${HACK_DIR}/add-virtual-instance.sh" "${web_id}" "${ADMIN_PASSWORD}"

	"${HACK_DIR}/allow-namespace.sh" "${cx_namespace}"

	log_step "Deploying ${#SCENARIO_SAMPLES[@]} client extensions into ${cx_namespace}"

	# Each instance's client extensions need distinct hostnames, and the CoreDNS
	# wildcard resolves any depth under localtest.me, so the instance's own
	# subdomain is enough to keep them apart.
	PUBLIC_DOMAIN_SUFFIX="${cx_namespace#liferay-}.localtest.me" \
		VIRTUAL_INSTANCE_ID="${web_id}" \
		"${HACK_DIR}/deploy-samples.sh" "${cx_namespace}" "${LIFERAY_NAMESPACE}" \
		"${SCENARIO_SAMPLES[@]}"

	_wait_for_ready "${cx_namespace}"

	"${HACK_DIR}/verify-handshake.sh" "${cx_namespace}" "${web_id}"

	_compare_instances "${cx_namespace}" "${web_id}"
}

# _compare_instances is the assertion that matters for this scenario: the same
# serviceId deployed twice must hold different credentials, because Liferay
# registered a separate OAuth2 application per virtual instance.
function _compare_instances {
	local cx_namespace=${1}
	local web_id=${2}

	log_step "Credentials are per virtual instance"

	printf '  %-32s %-40s %s\n' SERVICE "${VIRTUAL_INSTANCE_ID}" "${web_id}"

	local service_id

	for service_id in "${SCENARIO_SAMPLES[@]}"
	do
		local first second

		first=$(_client_id "${LIFERAY_NAMESPACE}" "${service_id}")
		second=$(_client_id "${cx_namespace}" "${service_id}")

		[ -z "${first}${second}" ] && continue

		printf '  %-32s %-40s %s\n' \
			"${service_id}" "${first:-<none>}" "${second:-<none>}"

		if [ -n "${first}" ] && [ "${first}" == "${second}" ]
		then
			echo "  FAIL      ${service_id} shares a client id across instances" >&2

			return 1
		fi
	done

	echo
	log "Scenario complete"
}

function _client_id {
	local namespace=${1}
	local service_id=${2}

	kube --namespace "${namespace}" get secret "${service_id}-lxc-ext-init" \
		--output json 2> /dev/null |
		python3 -c '
import base64, json, sys

data = json.load(sys.stdin).get("data", {})

print(next(
    (
        base64.b64decode(value).decode()
        for key, value in data.items()
        if key.endswith("user.agent.client.id")
    ),
    "",
))' 2> /dev/null || true
}

function _wait_for_ready {
	local cx_namespace=${1}

	log "Waiting for ${#SCENARIO_SAMPLES[@]} client extensions to reach Ready"

	wait_for "client extensions in ${cx_namespace}" 600 _all_ready "${cx_namespace}"
}

function _all_ready {
	local ready

	ready=$(kube --namespace "${1}" get clientextension \
		--output jsonpath='{range .items[*]}{.status.phase}{"\n"}{end}' |
		grep --count Ready || true)

	[ "${ready}" -ge "${#SCENARIO_SAMPLES[@]}" ]
}

main "${@}"
