#!/usr/bin/env bash
#
# Validate node-limit enforcement against the acceptance criteria. Run after
# 04-apply-samples.sh (environment fixtures applied, mock NOT yet enabled). This
# script drives the license state itself: it checks the fail-closed case first,
# then enables the mock and checks the enforced-ceiling cases.
#
#   AC#6  fail-closed: the governed workload is denied while no license is known
#   AC#3  within-limit create and scale are allowed
#   AC#4  over-limit create and over-limit scale are denied
#   AC#5  the denial message states requested vs. licensed

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

require_cmd kubectl

WORKLOAD="${MANIFESTS_DIR}/workload/liferay-statefulset.yaml"
FAILED=0

pass() { printf '  \033[1;32mPASS\033[0m %s\n' "$*"; }
miss() { printf '  \033[1;31mFAIL\033[0m %s\n' "$*"; FAILED=1; }

log "AC#6 — fail-closed: creating the workload before a license is known should be DENIED."
out="$(kc apply -f "${WORKLOAD}" 2>&1 || true)"
if grep -q "not yet available" <<<"${out}"; then
	pass "workload create denied while ceiling unknown (fail-closed)"
else
	miss "workload create not denied as expected: ${out}"
fi
kc delete -f "${WORKLOAD}" --ignore-not-found >/dev/null 2>&1 || true

log "Enabling the mock so a licensed ceiling (maxClusterNodes=2) becomes known."
"${HACK_DIR}/06-enable-mock.sh" >/dev/null
kc -n "${TEST_NAMESPACE}" wait --for=jsonpath='{.status.license.maxClusterNodes}'=2 \
	liferayenvironment/default --timeout=90s ||
	fail "maxClusterNodes did not reach 2"

log "AC#3 — within-limit create (replicas=1 <= 2) should be ALLOWED."
if kc apply -f "${WORKLOAD}" >/dev/null 2>&1; then
	pass "within-limit create allowed"
else
	miss "within-limit create was denied"
fi

log "AC#3 — within-limit scale (2 <= 2) should be ALLOWED."
if kc -n "${TEST_NAMESPACE}" scale statefulset/liferay-dxp --replicas=2 >/dev/null 2>&1; then
	pass "within-limit scale allowed"
else
	miss "within-limit scale was denied"
fi

log "AC#4/#5 — over-limit scale (5 > 2) should be DENIED with a clear message."
out="$(kc -n "${TEST_NAMESPACE}" scale statefulset/liferay-dxp --replicas=5 2>&1 || true)"
if grep -q "exceeds licensed maxClusterNodes 2" <<<"${out}"; then
	pass "over-limit scale denied: ${out#*denied the request: }"
else
	miss "over-limit scale not denied as expected: ${out}"
fi

log "AC#4 — over-limit create (replicas=5 > 2) should be DENIED."
kc -n "${TEST_NAMESPACE}" delete statefulset/liferay-dxp --wait >/dev/null 2>&1 || true
out="$(sed 's/replicas: 1/replicas: 5/' "${WORKLOAD}" | kc apply -f - 2>&1 || true)"
if grep -q "exceeds licensed maxClusterNodes 2" <<<"${out}"; then
	pass "over-limit create denied"
else
	miss "over-limit create not denied as expected: ${out}"
fi
kc apply -f "${WORKLOAD}" >/dev/null 2>&1 || true

echo
if [[ "${FAILED}" -eq 0 ]]; then
	log "All enforcement checks passed."
else
	fail "Some enforcement checks failed (see above)."
fi