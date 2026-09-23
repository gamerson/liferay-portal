#!/bin/bash

# Shared settings and helpers for the client extension spike scripts.

CLUSTER_NAME="${CLUSTER_NAME:-cx-spike}"

CX_NAMESPACE_DENIED="${CX_NAMESPACE_DENIED:-team-b}"

CX_NAMESPACE_SPLIT="${CX_NAMESPACE_SPLIT:-team-a}"

DXPSIM_IMAGE="${DXPSIM_IMAGE:-liferay/dxpsim:spike}"

IMAGE_TAG="${IMAGE_TAG:-spike}"

LIFERAY_IMAGE="${LIFERAY_IMAGE:-liferay/dxp:latest}"

LIFERAY_NAMESPACE="${LIFERAY_NAMESPACE:-liferay-prod}"

MARIADB_IMAGE="${MARIADB_IMAGE:-mariadb:11.4}"

MINIO_IMAGE="${MINIO_IMAGE:-minio/minio:latest}"

OPERATOR_IMAGE="${OPERATOR_IMAGE:-liferay/liferay-dxp-operator:spike}"

OPERATOR_NAMESPACE="${OPERATOR_NAMESPACE:-liferay-system}"

# The cluster bind mounts this directory, so it must exist before the cluster is
# created and must never be deleted and recreated afterwards: a replaced
# directory leaves the node holding a stale inode and the mount goes empty.
PORTAL_MODULES_DIR="${PORTAL_MODULES_DIR:-${HOME}/.liferay/cx-spike/portal-modules}"

SAMPLES_DIR="${SAMPLES_DIR:-$(cd "${HACK_DIR}/../../../workspaces/liferay-sample-workspace/client-extensions" && pwd)}"

VIRTUAL_INSTANCE_ID="${VIRTUAL_INSTANCE_ID:-liferay.localtest.me}"

function log {
	echo "==> ${*}"
}

function log_step {
	echo
	echo "############################################################"
	echo "# ${*}"
	echo "############################################################"
}

function kube {
	kubectl --context "k3d-${CLUSTER_NAME}" "${@}"
}

function helm_cx {
	helm --kube-context "k3d-${CLUSTER_NAME}" "${@}"
}

# sample_kind maps a sample project to the workload kind declared in its
# generated LCP.json.
function sample_kind {
	local sample=${1}

	unzip -p "${SAMPLES_DIR}/${sample}/dist/${sample}.zip" LCP.json 2> /dev/null |
		python3 -c 'import json,sys; print(json.load(sys.stdin).get("kind","Deployment"))' 2> /dev/null ||
		echo Deployment
}

# sample_schedule reads the CronJob schedule from a sample's LCP.json.
function sample_schedule {
	local sample=${1}

	unzip -p "${SAMPLES_DIR}/${sample}/dist/${sample}.zip" LCP.json 2> /dev/null |
		python3 -c 'import json,sys; print(json.load(sys.stdin).get("schedule",""))' 2> /dev/null ||
		echo ""
}

# sample_port reads the load balancer target port from a sample's LCP.json,
# defaulting to the Caddy port used by the static images.
function sample_port {
	local sample=${1}

	unzip -p "${SAMPLES_DIR}/${sample}/dist/${sample}.zip" LCP.json 2> /dev/null |
		python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("loadBalancer",{}).get("targetPort",80))' 2> /dev/null ||
		echo 80
}

# samples lists every sample project that produced a distributable zip.
function samples {
	local sample

	for sample in $(find "${SAMPLES_DIR}" -maxdepth 1 -mindepth 1 -type d -printf '%f\n' | sort)
	do
		if [ -f "${SAMPLES_DIR}/${sample}/dist/${sample}.zip" ]
		then
			echo "${sample}"
		fi
	done
}

# wait_for polls a command until it succeeds or the timeout expires.
function wait_for {
	local description=${1}
	local timeout=${2}
	shift 2

	local deadline=$((SECONDS + timeout))

	while [ "${SECONDS}" -lt "${deadline}" ]
	do
		if "${@}" > /dev/null 2>&1
		then
			return 0
		fi

		sleep 3
	done

	echo "timed out waiting for ${description}" >&2

	return 1
}