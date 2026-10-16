#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

#
# inspect_liferay.sh
#
# Prints what the overlay did to the running pod: what the link step linked and
# removed, what the main container copied or refused, what lives in the linked
# directories on the persistent volume, and what OSGi and the patching tool
# report now.
#

function main {
	_section "Link step"

	_get_link_log || echo "No link step ran."

	_section "Overlay copy and patching in the main container"

	_get_liferay_log | grep --extended-regexp "Copied|Skipping|[Pp]atch|hotfix" || true

	_section "Demo module"

	_get_liferay_log | grep "acme-overlay-demo" || echo "No demo module output."

	_section "Persistent volume"

	_exec_liferay '
		for dir in /opt/liferay/osgi/configs /opt/liferay/osgi/modules /mnt/liferay/patching
		do
			echo "${dir}:"

			ls -l "${dir}" | grep --invert-match "^total" | sed --expression "s/^/    /"
		done

		echo "/opt/liferay/tomcat/conf/acme-overlay.properties: $(cat /opt/liferay/tomcat/conf/acme-overlay.properties 2> /dev/null || echo missing)"
		echo "/opt/liferay/tomcat/lib/acme-overlay-README.txt: $(cat /opt/liferay/tomcat/lib/acme-overlay-README.txt 2> /dev/null || echo missing)"
		echo "/opt/liferay/portal-ext.properties has the overlay property: $(grep --count acme.overlay /opt/liferay/portal-ext.properties || true)"'

	_section "OSGi"

	_exec_liferay '
		(
			sleep 2
			echo "lb -s com.acme"
			sleep 2
			echo "scr:info com.acme.overlay.demo.DemoConfiguredComponent"
			sleep 2
			echo "disconnect"
			sleep 1
			echo "y"
		) | telnet localhost 11311 2> /dev/null | tr --delete "\r" | grep --extended-regexp "acme|State|greeting|No component"'

	_section "Patching tool"

	_exec_liferay "/opt/liferay/patching-tool/patching-tool.sh info 2> /dev/null | grep --extended-regexp \"Currently installed patch|hotfix\" || true"
}

function _exec_liferay {
	kube \
		--container liferay-default \
		--namespace "${NAMESPACE}" \
		exec \
		liferay-default-0 \
		-- \
		bash -c "${1}"
}

function _get_liferay_log {
	kube \
		--container liferay-default \
		--namespace "${NAMESPACE}" \
		logs \
		liferay-default-0
}

function _get_link_log {
	kube \
		--container liferay-prepopulate-overlay \
		--namespace "${NAMESPACE}" \
		logs \
		liferay-default-0 2> /dev/null | jq --raw-output '"\(.severity) \(.message)"'
}

function _section {
	echo ""
	echo "== ${1}"
	echo ""
}

main