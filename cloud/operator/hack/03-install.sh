#!/usr/bin/env bash
#
# Install (or upgrade) the operator chart into the cluster. Installs the CRD
# from the chart's crds/ directory and the manager Deployment, RBAC, and the
# self-signed webhook.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

require_cmd helm kubectl

log "Installing release '${RELEASE}' into namespace '${OPERATOR_NAMESPACE}'."

helm --kube-context "${KUBE_CONTEXT}" upgrade --install "${RELEASE}" "${HELM_CHART}" \
	--namespace "${OPERATOR_NAMESPACE}" \
	--create-namespace \
	--set image.repository="${IMAGE_REPOSITORY}" \
	--set image.tag="${IMAGE_TAG}" \
	--set image.pullPolicy=IfNotPresent \
	--wait \
	--timeout 120s

log "Waiting for the operator deployment to be available."

kc -n "${OPERATOR_NAMESPACE}" rollout status \
	"deployment/${RELEASE}" --timeout=120s

log "Operator installed."

kc -n "${OPERATOR_NAMESPACE}" get pods
