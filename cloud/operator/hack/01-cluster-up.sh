#!/usr/bin/env bash
#
# Create the k3d test cluster (idempotent). Safe to re-run.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

require_cmd k3d kubectl

if k3d cluster list "${CLUSTER_NAME}" >/dev/null 2>&1; then
	log "Cluster '${CLUSTER_NAME}' already exists; skipping create."
else
	log "Creating k3d cluster '${CLUSTER_NAME}'."

	k3d cluster create "${CLUSTER_NAME}" \
		--agents 2 \
		--wait
fi

log "Waiting for nodes to become Ready."

kc wait --for=condition=Ready nodes --all --timeout=120s

log "Cluster ready. Context: ${KUBE_CONTEXT}"

kc get nodes
