#!/bin/bash

# Points the demo domain at the cluster's load balancer inside the cluster.
#
# The virtual instance publishes one domain and every caller uses it: the
# browser, the Liferay pod calling a client extension, and a client extension
# calling Liferay. Resolving the whole suffix to the load balancer means the
# ingress controller routes all three the same way, and nothing here names a
# Service or a ClusterIP, so it survives redeploys.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

DNS_UPSTREAM="${DNS_UPSTREAM:-9.9.9.9 8.8.8.8}"

DOMAIN_SUFFIX="${PUBLIC_DOMAIN_SUFFIX:-localtest.me}"

# _forward_upstream points CoreDNS at public resolvers instead of the node's
# own resolv.conf.
#
# k3d nodes resolve through the Docker bridge gateway, and when Docker's
# embedded resolver stops forwarding, every outbound name lookup inside the
# cluster fails while the host resolves fine. That surfaces as a client
# extension answering 500 on a call to a third party API, which reads like a
# fault in the extension rather than in the cluster.
function _forward_upstream {
	local corefile

	corefile=$(kube --namespace kube-system get configmap coredns \
		--output jsonpath='{.data.Corefile}')

	if [[ "${corefile}" != *"forward . /etc/resolv.conf"* ]]
	then
		log "CoreDNS already forwards to an explicit upstream"

		return
	fi

	log "Forwarding CoreDNS to ${DNS_UPSTREAM}"

	kube --namespace kube-system patch configmap coredns --type merge --patch "$(
		DNS_UPSTREAM="${DNS_UPSTREAM}" COREFILE="${corefile}" python3 -c '
import json, os

print(json.dumps({
    "data": {
        "Corefile": os.environ["COREFILE"].replace(
            "forward . /etc/resolv.conf",
            "forward . " + os.environ["DNS_UPSTREAM"],
        )
    }
}))'
	)" > /dev/null

	kube --namespace kube-system rollout restart deployment/coredns
	kube --namespace kube-system rollout status deployment/coredns --timeout 120s
}

function main {
	_forward_upstream

	log_step "Resolving *.${DOMAIN_SUFFIX} to the load balancer"

	local gateway_ip

	gateway_ip=$(_gateway_ip)

	if [ -z "${gateway_ip}" ]
	then
		echo "Unable to find the load balancer for cluster ${CLUSTER_NAME}" >&2

		return 1
	fi

	log "Load balancer is ${gateway_ip}"

	sed \
		--expression "s|__DOMAIN_SUFFIX__|${DOMAIN_SUFFIX}|g" \
		--expression "s|__GATEWAY_IP__|${gateway_ip}|g" \
		"${HACK_DIR}/manifests/coredns-custom.yaml" | kube apply --filename -

	kube --namespace kube-system rollout restart deployment/coredns
	kube --namespace kube-system rollout status deployment/coredns --timeout 120s
}

function _gateway_ip {
	k3d cluster list "${CLUSTER_NAME}" --output json |
		python3 -c '
import json
import sys

cluster_name = sys.argv[1]

for cluster in json.load(sys.stdin):
	for node in cluster.get("nodes", []):
		labels = node.get("runtimeLabels", {})

		if labels.get("k3d.server.loadbalancer") == "k3d-%s-serverlb" % cluster_name:
			print(node["IP"]["IP"])

			break
' "${CLUSTER_NAME}"
}

main "${@}"