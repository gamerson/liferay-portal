#!/usr/bin/env bash
#
# Apply the test fixtures: a namespace, an activation-code Secret, a dummy
# Liferay StatefulSet to act as the workloadRef target, and a LiferayEnvironment
# CR to drive the reconcile loop.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

require_cmd kubectl

log "Applying fixtures from ${MANIFESTS_DIR}."

kc apply -f "${MANIFESTS_DIR}"

log "Waiting for the LiferayEnvironment to be observed."

# Give the controller a moment to run its first reconcile.
kc -n "${TEST_NAMESPACE}" wait --for=jsonpath='{.status.environmentId}' \
	liferayenvironment/default --timeout=60s ||
	warn "environmentId not populated yet; check 'operator logs' via 05-verify.sh"

log "Fixtures applied."

kc -n "${TEST_NAMESPACE}" get liferayenvironment,statefulset,secret
