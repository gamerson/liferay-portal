#!/bin/bash

# Points the browser facing domains at their in-cluster Services.
#
# The virtual instance publishes one main domain, and both halves use it: the
# browser reaches the portal on it, and a client extension calls the portal on
# it from inside the cluster. A name that only resolves on the developer's
# machine resolves to the pod itself inside the cluster, so it has to resolve
# in the cluster too. The prototype chart solved this with a CoreDNS hook; this
# is the same idea against k3s's NodeHosts.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

function main {
	log_step "Mapping public domains to cluster Services"

	local entries=""

	entries+="$(_entry liferay "${VIRTUAL_INSTANCE_ID}")"$'\n'

	local service

	for service in $(kube --namespace "${LIFERAY_NAMESPACE}" get service \
		--output name | sed 's|service/||' | grep '^liferay-sample-')
	do
		entries+="$(_entry "${service}" "${service}.${PUBLIC_DOMAIN_SUFFIX}")"$'\n'
	done

	local node_hosts

	node_hosts=$(kube --namespace kube-system get configmap coredns \
		--output go-template='{{index .data "NodeHosts"}}' |
		grep -v "${PUBLIC_DOMAIN_SUFFIX}" || true)

	kube --namespace kube-system patch configmap coredns \
		--type merge \
		--patch "$(python3 -c '
import json
import sys

print(json.dumps({"data": {"NodeHosts": sys.argv[1].rstrip() + "\n" + sys.argv[2]}}))
' "${node_hosts}" "${entries}")"

	kube --namespace kube-system rollout restart deployment/coredns

	kube --namespace kube-system rollout status deployment/coredns --timeout 120s

	echo "${entries}"
}

function _entry {
	local service=${1}
	local hostname=${2}

	local cluster_ip

	cluster_ip=$(kube --namespace "${LIFERAY_NAMESPACE}" get service "${service}" \
		--output go-template='{{.spec.clusterIP}}' 2> /dev/null || true)

	if [ -n "${cluster_ip}" ]
	then
		printf '%s %s' "${cluster_ip}" "${hostname}"
	fi
}

PUBLIC_DOMAIN_SUFFIX="${PUBLIC_DOMAIN_SUFFIX:-localtest.me}"

main "${@}"