#!/usr/bin/env bash
#
# Delete the k3d test cluster. Pass KEEP_CLUSTER=1 to only remove the release
# and fixtures while leaving the cluster running.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

require_cmd k3d

if [[ "${KEEP_CLUSTER:-0}" == "1" ]]; then
	log "KEEP_CLUSTER=1: removing fixtures and release, keeping cluster."

	kc delete -f "${MANIFESTS_DIR}" --ignore-not-found
	helm --kube-context "${KUBE_CONTEXT}" uninstall "${RELEASE}" \
		--namespace "${OPERATOR_NAMESPACE}" --ignore-not-found

	exit 0
fi

if k3d cluster list "${CLUSTER_NAME}" >/dev/null 2>&1; then
	log "Deleting k3d cluster '${CLUSTER_NAME}'."

	k3d cluster delete "${CLUSTER_NAME}"
else
	log "Cluster '${CLUSTER_NAME}' not found; nothing to delete."
fi
