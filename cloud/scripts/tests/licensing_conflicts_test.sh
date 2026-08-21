#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")/../dev" && pwd)/setup_k3d.sh"

function main {
	_check_utils kubectl

	if ! _environment_is_ready
	then
		echo "The environment is not ready. Run cloud/scripts/dev/setup_k3d.sh up first." >&2

		exit 1
	fi

	local fail=0
	local pass=0

	_run_test _test_operator_restores_the_ceiling_with_the_exemption
	_run_test _test_operator_restores_the_ceiling_without_the_exemption
	_run_test _test_argocd_converges_under_the_ceiling

	_restore_baseline

	echo ""
	echo "Results: ${pass} passed, ${fail} failed."

	if [[ ${fail} -eq 0 ]]
	then
		return 0
	fi

	echo ""
	echo "Both known conflicts are expected to fail until their fixes land. The operator"
	echo "one fails because the operator writes the workload before it persists the status"
	echo "that its own admission policy reads, so a restored license never takes effect."
	echo "The ArgoCD one fails because the policy denies every over-limit write, so a"
	echo "repository that asks for more replicas than the license allows can never sync."

	return 1
}

function _application_message {
	_kubectl get application "${_ENVIRONMENT_NAMESPACE}" --namespace "${_ARGOCD_NAMESPACE}" --output jsonpath="{.status.operationState.message}" 2> /dev/null
}

function _assert_the_ceiling_is_restored {
	_configure_license '{"licenseOwner": "some-other-environment-uid", "maxClusterNodes": 3}'

	if ! _wait_until "the operator to block the workload" _workload_replicas_are 0
	then
		echo "The workload never scaled to zero, so the mismatch was never enforced."

		return 1
	fi

	_configure_license '{"licenseOwner": null}'

	if ! _wait_until "the ceiling to be restored" _licensed_ceiling_is 3
	then
		echo "The ceiling is still $(_licensed_ceiling) after a valid license was restored."

		return 1
	fi
}

function _configure_license {
	_kubectl --namespace "${_MOCK_NAMESPACE}" exec deploy/provisioning-mock -- \
		wget \
			-O - \
			--post-data "${1}" \
			-q \
			http://127.0.0.1:8080/_config > /dev/null

	_nudge_environment
}

function _exempt_operator {
	local state=${1}

	if [[ ${state} == "off" ]]
	then
		_kubectl patch validatingadmissionpolicy liferay-dxp-operator-statefulset-scale --patch '{"spec": {"matchConditions": null}}' --type merge > /dev/null

		return
	fi

	helm upgrade \
		--install liferay-dxp-operator "${_ROOT_CLOUD_DIR}/helm/dxp-operator" \
		--kube-context "k3d-${_CLUSTER_NAME}" \
		--namespace "${_OPERATOR_NAMESPACE}" \
		--reuse-values \
		--timeout 5m \
		--wait > /dev/null
}

function _licensed_ceiling {
	_kubectl get liferayenvironment "${_ENVIRONMENT_NAME}" --namespace "${_ENVIRONMENT_NAMESPACE}" --output jsonpath="{.status.license.maxClusterNodes}" 2> /dev/null
}

function _licensed_ceiling_is {
	[[ $(_licensed_ceiling) == "${1}" ]]
}

function _nudge_environment {
	_kubectl annotate liferayenvironment "${_ENVIRONMENT_NAME}" "k3d.liferay.com/nudge=$(date +%s)" --namespace "${_ENVIRONMENT_NAMESPACE}" --overwrite > /dev/null
}

function _restore_baseline {
	echo ""
	echo "Restoring the licensed ceiling, the exemption, and the requested replicas."

	_exempt_operator on

	_configure_license '{"licenseOwner": null, "maxClusterNodes": 3}'

	_set_desired_replicas 3

	_wait_until "the environment to recover" _licensed_ceiling_is 3
}

function _run_test {
	local test_function=${1}

	local description=$(echo "${test_function}" | sed --expression "s/^_test_//" --expression "s/_/ /g")

	echo ""
	echo "RUN: ${description}."

	local exit_code=0

	"${test_function}" || exit_code=${?}

	if [[ ${exit_code} -eq 0 ]]
	then
		echo "PASS: ${description}."

		pass=$((pass + 1))
	else
		echo "FAIL: ${description}."

		fail=$((fail + 1))
	fi
}

function _set_desired_replicas {
	local count=${1}

	local pod=$(_gitserver_pod)

	_kubectl --namespace "${_MOCK_NAMESPACE}" exec "${pod}" -- sh -c "
		cd /srv/git/seed

		sed -i 's/^replicaCount: .*/replicaCount: ${count}/' chart/values-k3d.yaml

		if ! git diff --quiet
		then
			git commit --all --message 'Request ${count} replicas' --quiet
			git push --quiet /srv/git/environment.git main
		fi"

	_kubectl annotate application "${_ENVIRONMENT_NAMESPACE}" argocd.argoproj.io/refresh=hard --namespace "${_ARGOCD_NAMESPACE}" --overwrite > /dev/null
}

function _sync_environment {
	_kubectl annotate application "${_ENVIRONMENT_NAMESPACE}" argocd.argoproj.io/refresh=hard --namespace "${_ARGOCD_NAMESPACE}" --overwrite > /dev/null || true

	_kubectl \
		patch application "${_ENVIRONMENT_NAMESPACE}" \
		--namespace "${_ARGOCD_NAMESPACE}" \
		--patch '{"operation": {"initiatedBy": {"username": "licensing_conflicts_test"}, "sync": {"revision": "main"}}}' \
		--type merge &> /dev/null || true
}

function _sync_has_finished {
	local phase=$(_kubectl get application "${_ENVIRONMENT_NAMESPACE}" --namespace "${_ARGOCD_NAMESPACE}" --output jsonpath="{.status.operationState.phase}" 2> /dev/null)

	[[ ${phase} != "Running" ]]
}

function _test_argocd_converges_under_the_ceiling {
	_exempt_operator on

	_set_desired_replicas 3

	_configure_license '{"licenseOwner": null, "maxClusterNodes": 1}'

	if ! _wait_until "the operator to cap the workload" _workload_replicas_are 1
	then
		echo "The workload was never capped, so the ceiling was never enforced."

		return 1
	fi

	echo "The repository asks for 3 replicas and the license allows 1."

	_sync_environment

	_wait_until "the sync to finish" _sync_has_finished

	local message=$(_application_message)

	if [[ ${message} == *"denied request: replicas"* ]]
	then
		echo "The sync was denied: ${message}"

		return 1
	fi

	_workload_replicas_are 1
}

function _test_operator_restores_the_ceiling_with_the_exemption {
	_exempt_operator on

	_assert_the_ceiling_is_restored
}

function _test_operator_restores_the_ceiling_without_the_exemption {
	_exempt_operator off

	_assert_the_ceiling_is_restored
}

function _wait_until {
	local description=${1}

	shift

	echo "Waiting for ${description}."

	local attempt=0

	while [[ ${attempt} -lt 24 ]]
	do
		if "${@}" &> /dev/null
		then
			return 0
		fi

		attempt=$((attempt + 1))

		sleep 5
	done

	return 1
}

function _workload_replicas_are {
	local replicas=$(_kubectl get statefulset "${_ENVIRONMENT_NAME}" --namespace "${_ENVIRONMENT_NAMESPACE}" --output jsonpath="{.spec.replicas}" 2> /dev/null)

	[[ ${replicas} == "${1}" ]]
}

main "${@}"
