#!/bin/bash

# Adds a namespace to spec.clientExtensionNamespaces on the LiferayEnvironment.
#
# allow-namespace.sh <namespace> [liferay-environment]
#
# Delivery into a namespace other than Liferay's own is consented to by the
# Liferay side, not claimed by the client extension: a ClientExtension in an
# unlisted namespace stays Degraded with EnvironmentUnusable. This is the
# consent.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

function main {
	local namespace=${1:?usage: allow-namespace.sh <namespace> [liferay-environment]}
	local environment=${2:-}

	if [ -z "${environment}" ]
	then
		environment=$(kube --namespace "${LIFERAY_NAMESPACE}" get liferayenvironment \
			--output jsonpath='{.items[0].metadata.name}')
	fi

	log_step "Allowing ${namespace} on LiferayEnvironment ${environment}"

	local patch

	patch=$(kube --namespace "${LIFERAY_NAMESPACE}" get liferayenvironment "${environment}" \
		--output json |
		python3 -c '
import json, sys

namespace = sys.argv[1]
allowed = json.load(sys.stdin)["spec"].get("clientExtensionNamespaces") or []

if namespace in allowed:
    print("")
else:
    print(json.dumps(
        {"spec": {"clientExtensionNamespaces": sorted(allowed + [namespace])}}
    ))' "${namespace}")

	if [ -z "${patch}" ]
	then
		log "${namespace} is already allowed"

		return
	fi

	kube --namespace "${LIFERAY_NAMESPACE}" patch liferayenvironment "${environment}" \
		--type merge --patch "${patch}" > /dev/null

	log "Allowed: $(
		kube --namespace "${LIFERAY_NAMESPACE}" get liferayenvironment "${environment}" \
			--output jsonpath='{.spec.clientExtensionNamespaces}'
	)"
}

main "${@}"
