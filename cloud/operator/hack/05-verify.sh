#!/usr/bin/env bash
#
# Observe the results of the reconcile loop. With no provisioning backend wired,
# the expected steady state is:
#
#   - status.environmentId  = the acme-prod namespace UID
#   - a Secret "default-cne-identity" created (the generated cluster keypair)
#   - condition ProvisioningReachable = False (reason NoProvisioningClient)
#   - status.phase          = Pending
#
# Activation, entitlements, replica clamping, and app download stay dormant until
# a provisioning backend (or the mock) is wired in — see README.md.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

require_cmd kubectl

log "LiferayEnvironment resources:"
kc get liferayenvironment -A

log "LiferayEnvironment 'default' status:"
kc -n "${TEST_NAMESPACE}" get liferayenvironment/default -o yaml |
	sed -n '/^status:/,$p'

log "Generated identity Secret (expect default-cne-identity):"
kc -n "${TEST_NAMESPACE}" get secret -o name | grep -i identity ||
	warn "identity Secret not found yet"

log "Events in ${TEST_NAMESPACE}:"
kc -n "${TEST_NAMESPACE}" get events --sort-by=.lastTimestamp | tail -20

log "Operator logs (last 40 lines):"
kc -n "${OPERATOR_NAMESPACE}" logs "deployment/${RELEASE}" --tail=40
