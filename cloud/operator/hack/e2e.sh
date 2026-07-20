#!/usr/bin/env bash
#
# Run the full local flow end to end: cluster up, build+load image, install the
# operator, apply the test fixtures, and print the reconcile results.
#
# Teardown is intentionally NOT run here so you can inspect the cluster
# afterward. Run 99-teardown.sh when finished.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

require_cmd k3d docker kubectl helm

"${HACK_DIR}/01-cluster-up.sh"
"${HACK_DIR}/02-build-load.sh"
"${HACK_DIR}/03-install.sh"
"${HACK_DIR}/04-apply-samples.sh"
"${HACK_DIR}/05-verify.sh"

log "Done. Inspect with kubectl (context ${KUBE_CONTEXT}); tear down with 99-teardown.sh."
