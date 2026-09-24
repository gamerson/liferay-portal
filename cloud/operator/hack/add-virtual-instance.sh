#!/bin/bash

# Adds a Liferay virtual instance, routes its virtual host to the Liferay
# service, and waits for the portal-k8s-agent to publish its dxp metadata.
#
# add-virtual-instance.sh <web-id> [admin-password]
#
# The web ID is also the virtual host and the value the operator matches on
# through dxp.lxc.liferay.com/virtualInstanceId, so it must be a resolvable
# hostname -- anything under localtest.me resolves through the CoreDNS wildcard
# that patch-coredns.sh installs.
#
# Re-running for an existing web ID only reconciles the ingress and the wait.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

ADMIN_LOGIN="${ADMIN_LOGIN:-test@${VIRTUAL_INSTANCE_ID}}"

ADMIN_PASSWORD="${ADMIN_PASSWORD:-test}"

PORTAL_URL="${PORTAL_URL:-http://${VIRTUAL_INSTANCE_ID}}"

function main {
	local web_id=${1:?usage: add-virtual-instance.sh <web-id> [admin-password]}
	local admin_password=${2:-test}

	log_step "Adding virtual instance ${web_id}"

	if _company_exists "${web_id}"
	then
		log "Virtual instance ${web_id} already exists"
	else
		_add_company "${web_id}" "${admin_password}"
	fi

	_route "${web_id}"
	_wait_for_metadata "${web_id}"

	log "Virtual instance ${web_id} is ready at http://${web_id}"
}

function _add_company {
	local web_id=${1}
	local admin_password=${2}

	log "Submitting the add instance form (this takes a few minutes)"

	python3 "${HACK_DIR}/add_virtual_instance.py" \
		"${PORTAL_URL}" "${ADMIN_LOGIN}" "${ADMIN_PASSWORD}" \
		"${web_id}" "${admin_password}"

	wait_for "company ${web_id}" 1800 _company_exists "${web_id}"

	log "Created ${web_id}"
}

function _company_exists {
	local web_id=${1}

	curl --fail --silent --user "${ADMIN_LOGIN}:${ADMIN_PASSWORD}" \
		--url "${PORTAL_URL}/api/jsonws/company/get-companies" |
		python3 -c '
import json, sys

web_id = sys.argv[1]

sys.exit(
    0 if any(c["webId"] == web_id for c in json.load(sys.stdin)) else 1
)' "${web_id}"
}

# _route adds the virtual host to the Liferay ingress. Liferay answers a request
# whose Host matches no virtual host with a 500, so without this the instance is
# unreachable from both the browser and the client extension pods.
function _route {
	local web_id=${1}

	if kube --namespace "${LIFERAY_NAMESPACE}" get ingress liferay \
		--output jsonpath='{.spec.rules[*].host}' | grep --quiet --word-regexp "${web_id}"
	then
		log "Ingress already routes ${web_id}"

		return
	fi

	log "Routing ${web_id} to the liferay service"

	kube --namespace "${LIFERAY_NAMESPACE}" patch ingress liferay --type json \
		--patch "$(
			python3 -c '
import json, sys

print(json.dumps([{
    "op": "add",
    "path": "/spec/rules/-",
    "value": {
        "host": sys.argv[1],
        "http": {
            "paths": [{
                "backend": {
                    "service": {"name": "liferay", "port": {"number": 80}}
                },
                "path": "/",
                "pathType": "Prefix",
            }]
        },
    },
}]))' "${web_id}"
		)" > /dev/null

	wait_for "http://${web_id} to answer" 120 \
		curl --fail --output /dev/null --silent --url "http://${web_id}/"
}

# _wait_for_metadata waits for the agent to publish the ConfigMap the operator
# keys every client extension off. It appears on its own; nothing here creates it.
function _wait_for_metadata {
	local web_id=${1}

	wait_for "${web_id}-lxc-dxp-metadata" 600 \
		kube --namespace "${LIFERAY_NAMESPACE}" get configmap \
		"${web_id}-lxc-dxp-metadata"

	log "Agent published ${web_id}-lxc-dxp-metadata"
}

main "${@}"
