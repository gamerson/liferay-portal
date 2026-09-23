#!/bin/bash

# Drives an object action from Liferay into a client extension microservice and
# proves the microservice received it.
#
# verify-object-action.sh [cx-namespace] [virtual-instance-id]
#
# Requires liferay-sample-batch to have been deployed: it provisions the Sample
# object definition along with the four onAfterUpdate actions that point at the
# node and Spring Boot samples.
#
# The assertion is the microservice's own log, not the action status Liferay
# records. That status reports that the request was dispatched, and stays
# "success" even when the endpoint answers 500.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

ADMIN_PASSWORD="${ADMIN_PASSWORD:-test}"

# Unique per run, so the assertion can scan the whole log rather than count
# lines. An absolute offset into a container log is not a stable thing to hold:
# the log keeps growing while the action is in flight and rotates out from under
# a long lived environment.
PROBE="object-action-probe-$(date +%s)-${RANDOM}"

SINCE="${SINCE:-3m}"

function main {
	local cx_namespace=${1:-${LIFERAY_NAMESPACE}}
	local virtual_instance_id=${2:-${VIRTUAL_INSTANCE_ID}}

	local portal="http://${virtual_instance_id}"
	local credentials="test@${virtual_instance_id}:${ADMIN_PASSWORD}"

	log_step "Firing an object action into ${cx_namespace}"

	_require_object "${portal}" "${credentials}"

	local entry_id
	entry_id=$(_add_entry "${portal}" "${credentials}")

	log "Created Sample entry ${entry_id}; updating it to fire onAfterUpdate"

	curl --fail --silent --user "${credentials}" \
		--header "Content-Type: application/json" \
		--request PATCH \
		--url "${portal}/o/c/samples/${entry_id}" \
		--data "{\"field1\":\"${PROBE}\"}" > /dev/null

	_assert_received "${cx_namespace}"
}

# _assert_received waits for this run's probe value to appear in the handler's
# own log. Liferay dispatches the action asynchronously, so the update returning
# proves nothing about whether the microservice was reached.
function _assert_received {
	local cx_namespace=${1}

	local deadline=$((SECONDS + 180))

	while [ "${SECONDS}" -lt "${deadline}" ]
	do
		if _handler_log "${cx_namespace}" | grep --quiet "${PROBE}"
		then
			log "Microservice logged the payload"

			_handler_log "${cx_namespace}" |
				grep --extended-regexp "objectActionTriggerKey|${PROBE}" |
				head -4 |
				sed 's/^/    /'

			_report_errors "${cx_namespace}"

			return
		fi

		sleep 5
	done

	echo "  FAIL      no log line carrying ${PROBE} within 180s" >&2

	return 1
}

function _add_entry {
	local portal=${1}
	local credentials=${2}

	curl --fail --silent --user "${credentials}" \
		--header "Content-Type: application/json" \
		--request POST \
		--url "${portal}/o/c/samples" \
		--data '{"field1":"created by verify-object-action.sh"}' |
		python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])'
}

# _handler_log reads a time window rather than the whole log, which keeps the
# error report scoped to this run without depending on line positions.
function _handler_log {
	kube --namespace "${1}" logs deployment/liferay-sample-etc-spring-boot \
		--since "${SINCE}" 2> /dev/null
}

# _report_errors surfaces exceptions the handler raised. Liferay would record the
# action as successful regardless, so this is the only place they show up.
function _report_errors {
	local errors

	errors=$(_handler_log "${1}" |
		grep --count --extended-regexp "Exception|ERROR" || true)

	if [ "${errors}" -gt 0 ]
	then
		echo "  WARN      ${errors} error line(s) in the last ${SINCE} of handler log:"

		_handler_log "${1}" |
			grep --extended-regexp "Exception|ERROR" |
			head -3 |
			sed 's/^/    /'
	else
		echo "  ok        no exceptions raised while handling it"
	fi
}

function _require_object {
	local portal=${1}
	local credentials=${2}

	if ! curl --fail --silent --user "${credentials}" \
		--url "${portal}/o/c/samples?pageSize=1" > /dev/null
	then
		echo "The Sample object is absent. Deploy liferay-sample-batch first:" >&2
		echo "  deploy-samples.sh <cx-namespace> ${LIFERAY_NAMESPACE} liferay-sample-batch" >&2

		return 1
	fi
}

main "${@}"
