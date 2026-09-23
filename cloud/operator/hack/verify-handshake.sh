#!/bin/bash

# Asserts the configuration handshake for every client extension in a namespace,
# then calls the microservice endpoints with a real OAuth2 token.
#
# verify-handshake.sh [cx-namespace] [virtual-instance-id]
#
# The two halves are checked separately because they fail for different reasons.
# The ConfigMap checks prove the operator and the agent did their work; the token
# checks prove the credentials that came back are the ones the workload is
# actually using.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

ADMIN_PASSWORD="${ADMIN_PASSWORD:-test}"

FAILURES=0

function main {
	local cx_namespace=${1:-${LIFERAY_NAMESPACE}}
	local virtual_instance_id=${2:-${VIRTUAL_INSTANCE_ID}}

	log_step "Verifying ${cx_namespace} against ${virtual_instance_id}"

	_check_phases "${cx_namespace}"
	_check_dxp_metadata "${cx_namespace}" "${virtual_instance_id}"
	_check_ext_init "${cx_namespace}" "${virtual_instance_id}"
	_check_tokens "${cx_namespace}" "${virtual_instance_id}"

	if [ "${FAILURES}" -gt 0 ]
	then
		log "${FAILURES} check(s) failed"

		return 1
	fi

	log "All checks passed"
}

function _check {
	local description=${1}
	shift

	if "${@}" > /dev/null 2>&1
	then
		echo "  ok        ${description}"
	else
		echo "  FAIL      ${description}"

		FAILURES=$((FAILURES + 1))
	fi
}

# _check_dxp_metadata confirms the routes reached the client extension namespace.
# When it equals Liferay's own the agent's ConfigMap is read in place, so there is
# deliberately no mirror to look for.
function _check_dxp_metadata {
	local cx_namespace=${1}
	local virtual_instance_id=${2}

	local name="${virtual_instance_id}-lxc-dxp-metadata"

	_check "agent published ${name}" \
		kube --namespace "${LIFERAY_NAMESPACE}" get configmap "${name}"

	if [ "${cx_namespace}" == "${LIFERAY_NAMESPACE}" ]
	then
		echo "  --        same namespace; no mirror expected"

		return
	fi

	_check "mirrored ${name} into ${cx_namespace}" \
		kube --namespace "${cx_namespace}" get configmap "${name}"

	local source
	source=$(kube --namespace "${cx_namespace}" get configmap "${name}" \
		--output jsonpath='{.metadata.annotations.cx\.liferay\.com/source}' 2> /dev/null || true)

	if [ "${source}" == "${LIFERAY_NAMESPACE}/${name}" ]
	then
		echo "  ok        mirror names its source ${source}"
	else
		echo "  FAIL      mirror source is ${source:-<unset>}"

		FAILURES=$((FAILURES + 1))
	fi

	if diff \
		<(kube --namespace "${LIFERAY_NAMESPACE}" get configmap "${name}" --output jsonpath='{.data}') \
		<(kube --namespace "${cx_namespace}" get configmap "${name}" --output jsonpath='{.data}') \
		> /dev/null
	then
		echo "  ok        mirror content matches the source"
	else
		echo "  FAIL      mirror content drifted from the source"

		FAILURES=$((FAILURES + 1))
	fi
}

# _check_ext_init follows the return leg: Liferay writes the credentials into its
# own namespace and the operator mirrors them into a Secret beside the workload.
function _check_ext_init {
	local cx_namespace=${1}
	local virtual_instance_id=${2}

	local service_id

	for service_id in $(_service_ids "${cx_namespace}")
	do
		local secret

		secret=$(kube --namespace "${cx_namespace}" get clientextension "${service_id}" \
			--output jsonpath='{.status.extInitSecretName}' 2> /dev/null || true)

		if [ -z "${secret}" ]
		then
			echo "  --        ${service_id} declares no OAuth2 application"

			continue
		fi

		_check "Liferay wrote ${service_id}-${virtual_instance_id}-lxc-ext-init-metadata" \
			kube --namespace "${LIFERAY_NAMESPACE}" get configmap \
			"${service_id}-${virtual_instance_id}-lxc-ext-init-metadata"

		_check "mirrored credentials into ${cx_namespace}/${secret}" \
			kube --namespace "${cx_namespace}" get secret "${secret}"
	done
}

function _check_phases {
	local cx_namespace=${1}

	local line

	while read -r line
	do
		[ -z "${line}" ] && continue

		local name=${line%% *}
		local phase=${line##* }

		if [ "${phase}" == "Ready" ]
		then
			echo "  ok        ${name} is Ready"
		else
			echo "  FAIL      ${name} is ${phase}"

			FAILURES=$((FAILURES + 1))
		fi
	done < <(kube --namespace "${cx_namespace}" get clientextension \
		--output jsonpath='{range .items[*]}{.metadata.name} {.status.phase}{"\n"}{end}')
}

# _check_tokens mints a token per OAuth2 application and calls the endpoint the
# client extension advertises, which is the only check that proves the mirrored
# credentials are the ones the running process accepts.
function _check_tokens {
	local cx_namespace=${1}
	local virtual_instance_id=${2}

	local -a targets=()
	local service_id

	for service_id in $(_service_ids "${cx_namespace}")
	do
		local secret

		secret=$(kube --namespace "${cx_namespace}" get clientextension "${service_id}" \
			--output jsonpath='{.status.extInitSecretName}' 2> /dev/null || true)

		[ -z "${secret}" ] && continue

		local probe

		probe=$(_probe_path "${service_id}")

		[ -z "${probe}" ] && continue

		local domain

		domain=$(kube --namespace "${cx_namespace}" get clientextension "${service_id}" \
			--output jsonpath='{.spec.domain}')

		targets+=("${service_id}-oaua=http://${domain}${probe}")
	done

	if [ ${#targets[@]} -eq 0 ]
	then
		echo "  --        no microservice endpoints to call"

		return
	fi

	if ! python3 "${HACK_DIR}/verify_oauth.py" \
		"http://${virtual_instance_id}" "test@${virtual_instance_id}" \
		"${ADMIN_PASSWORD}" "${targets[@]}"
	then
		FAILURES=$((FAILURES + 1))
	fi
}

# _probe_path maps a sample to an endpoint that answers a plain authenticated GET.
# Most microservice resource paths only accept the POST Liferay sends them, so
# only the samples with a readable endpoint are probed this way.
function _probe_path {
	case ${1} in
		liferay-sample-etc-node)
			echo "/comic"
			;;
		liferay-sample-etc-spring-boot)
			echo "/dad/joke"
			;;
		*)
			echo ""
			;;
	esac
}

function _service_ids {
	kube --namespace "${1}" get clientextension \
		--output jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}'
}

main "${@}"
