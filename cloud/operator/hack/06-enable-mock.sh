#!/usr/bin/env bash
#
# Deploy the mock provisioning server in-cluster and point the operator at it,
# so the full activation -> entitlements -> clamp path runs offline.
#
# The mock returns maxClusterNodes=2 (see mock-provisioning/deploy.yaml), so with
# the sample CR's desiredReplicas=3 you will see the workload clamped to 2 and
# the ReplicasClamped condition go True. A kubectl scale above 2 is then rejected
# by the validating webhook.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

require_cmd kubectl helm

log "Creating/updating the mock server ConfigMap."

kc -n "${OPERATOR_NAMESPACE}" create configmap mock-provisioning-src \
	--from-file=server.py="${HACK_DIR}/mock-provisioning/server.py" \
	--dry-run=client -o yaml | kc apply -f -

log "Deploying the mock provisioning server."

kc -n "${OPERATOR_NAMESPACE}" apply -f "${HACK_DIR}/mock-provisioning/deploy.yaml"
kc -n "${OPERATOR_NAMESPACE}" rollout status \
	deployment/mock-provisioning --timeout=120s

MOCK_URL="http://mock-provisioning.${OPERATOR_NAMESPACE}.svc:8888"

log "Pointing the operator at ${MOCK_URL}."

helm --kube-context "${KUBE_CONTEXT}" upgrade "${RELEASE}" "${HELM_CHART}" \
	--namespace "${OPERATOR_NAMESPACE}" \
	--reuse-values \
	--set provisioning.baseURL="${MOCK_URL}"

kc -n "${OPERATOR_NAMESPACE}" rollout status \
	"deployment/${RELEASE}" --timeout=120s

log "Nudging the CR to force an immediate reconcile."

kc -n "${TEST_NAMESPACE}" annotate liferayenvironment/default \
	licensing.liferay.com/reconcile-nudge="$(date +%s)" --overwrite

log "Mock enabled. Watch the reconcile results with 05-verify.sh."
log "Try the webhook: kubectl --context ${KUBE_CONTEXT} -n ${TEST_NAMESPACE} scale statefulset/liferay-dxp --replicas=5"
