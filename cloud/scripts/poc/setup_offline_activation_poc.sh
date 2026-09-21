#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

#
# Stands up a single node k3d cluster carrying everything the offline
# activation control panel needs: the LiferayEnvironment CRD, the DXP
# operator, Argo Workflows, and a DXP instance whose control panel can read
# the activation request, write a bundle onto the marketplace volume, and
# submit the workflow that activates from it.
#
# The marketplace volume is one host directory both the operator and DXP
# mount, which is how the shared filesystem behaves in a real deployment.
#

function main {
	_check_prerequisites

	_build_jars

	_create_cluster

	_install_argo_workflows

	_install_operator

	_install_liferay "${_OFFLINE_NAMESPACE}" "values-poc-offline.yaml"

	_install_liferay "${_ONLINE_NAMESPACE}" "values-poc-online.yaml"

	_report
}

function _build_jars {
	echo
	echo "==> Building the license activation bundles"
	echo

	mkdir --parents "${_JARS_DIR}"

	bash "${_SCRIPT_DIR}/build_license_activation_jars.sh" "${_JARS_DIR}"
}

function _check_prerequisites {
	local command_name

	for command_name in docker helm k3d kubectl
	do
		if ! command -v "${command_name}" > /dev/null
		then
			echo "${command_name} is required." >&2

			exit 1
		fi
	done
}

function _create_cluster {
	echo
	echo "==> Creating the k3d cluster ${_CLUSTER_NAME}"
	echo

	#
	# DXP and the operator both run as non root, and the kubelet creates the
	# namespace subdirectory as root on first mount, so the share has to be
	# writable by anyone before either of them touches it.
	#

	mkdir --parents \
		"${_MARKETPLACE_DIR}/${_OFFLINE_NAMESPACE}" \
		"${_MARKETPLACE_DIR}/${_ONLINE_NAMESPACE}"

	chmod 0777 \
		"${_MARKETPLACE_DIR}" \
		"${_MARKETPLACE_DIR}/${_OFFLINE_NAMESPACE}" \
		"${_MARKETPLACE_DIR}/${_ONLINE_NAMESPACE}"

	if k3d cluster list "${_CLUSTER_NAME}" > /dev/null 2>&1
	then
		k3d cluster start "${_CLUSTER_NAME}"
	else
		k3d cluster create "${_CLUSTER_NAME}" \
			--port "8080:80@loadbalancer" \
			--volume "${_JARS_DIR}:/liferay-jars@server:0" \
			--volume "${_MARKETPLACE_DIR}:/liferay-marketplace@server:0"
	fi

	kubectl config use-context "k3d-${_CLUSTER_NAME}"
}

function _install_argo_workflows {
	echo
	echo "==> Installing Argo Workflows"
	echo

	helm repo add argo https://argoproj.github.io/argo-helm > /dev/null 2>&1 || true

	helm repo update argo > /dev/null

	helm upgrade \
		--create-namespace \
		--install \
		--namespace "${_ARGO_NAMESPACE}" \
		--set "server.authModes={server}" \
		--set "server.extraArgs={--auth-mode=server}" \
		--version "${_ARGO_CHART_VERSION}" \
		--wait \
		argo-workflows argo/argo-workflows
}

function _install_liferay {
	local namespace=${1}
	local values_file=${2}

	echo
	echo "==> Installing Liferay DXP into ${namespace}"
	echo

	kubectl create namespace "${namespace}" \
		--dry-run=client --output yaml | kubectl apply --filename -

	kubectl label namespace "${namespace}" \
		--overwrite "licensing.liferay.com/environment=true"

	_install_marketplace_volume "${namespace}" "liferay-default-marketplace"

	kubectl create secret generic liferay-default-activation \
		--dry-run=client \
		--from-literal="activationCode=placeholder" \
		--namespace "${namespace}" \
		--output yaml | kubectl apply --filename -

	helm upgrade \
		--install \
		--namespace "${namespace}" \
		--values "${_SCRIPT_DIR}/values-poc.yaml" \
		--values "${_SCRIPT_DIR}/${values_file}" \
		liferay-default "${_CLOUD_DIR}/helm/default"
}

function _install_marketplace_volume {
	local namespace=${1}
	local claim_name=${2}

	#
	# Both claims resolve to the same host directory, so the operator and DXP
	# see one shared filesystem the way they do behind a CSI volume.
	#

	kubectl apply --filename - <<EOF
apiVersion: v1
kind: PersistentVolume
metadata:
	name: ${namespace}-marketplace
spec:
	accessModes:
		- ReadWriteMany
	capacity:
		storage: 1Gi
	claimRef:
		name: ${claim_name}
		namespace: ${namespace}
	hostPath:
		path: /liferay-marketplace
		type: DirectoryOrCreate
	persistentVolumeReclaimPolicy: Retain
	storageClassName: ""
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
	name: ${claim_name}
	namespace: ${namespace}
spec:
	accessModes:
		- ReadWriteMany
	resources:
		requests:
			storage: 1Gi
	storageClassName: ""
	volumeName: ${namespace}-marketplace
EOF
}

function _install_operator {
	echo
	echo "==> Building and installing the DXP operator"
	echo

	docker build --tag "${_OPERATOR_IMAGE}" "${_CLOUD_DIR}/operator"

	k3d image import --cluster "${_CLUSTER_NAME}" "${_OPERATOR_IMAGE}"

	kubectl create namespace "${_OPERATOR_NAMESPACE}" \
		--dry-run=client --output yaml | kubectl apply --filename -

	_install_marketplace_volume "${_OPERATOR_NAMESPACE}" "marketplace"

	helm upgrade \
		--install \
		--namespace "${_OPERATOR_NAMESPACE}" \
		--set "image.pullPolicy=Never" \
		--set "image.repository=liferay-dxp-operator-poc" \
		--set "image.tag=latest" \
		--set "marketplace.claimName=marketplace" \
		--set "marketplace.enabled=true" \
		--set "offlineActivationWorkflow.enabled=true" \
		--set "onlineActivationWorkflow.enabled=true" \
		--set "provisioning.baseURL=${_PROVISIONING_BASE_URL}" \
		--wait \
		liferay-dxp-operator "${_CLOUD_DIR}/helm/dxp-operator"
}

function _report {
	cat <<EOF

================================================================
The activation PoC is up, with one offline and one online environment.

Argo Workflows UI:
    kubectl --namespace ${_ARGO_NAMESPACE} port-forward svc/argo-workflows-server 2746:2746
    open http://localhost:2746

Offline environment (${_OFFLINE_NAMESPACE}):
    kubectl --namespace ${_OFFLINE_NAMESPACE} port-forward svc/liferay-default 8888:8080
    open http://localhost:8888

Online environment (${_ONLINE_NAMESPACE}):
    kubectl --namespace ${_ONLINE_NAMESPACE} port-forward svc/liferay-default 8889:8080
    open http://localhost:8889

Sign in as test@liferay.com with the password test, then open
Control Panel -> Configuration -> License Manager -> Activation.

The forward port has to match what each instance was told, because the
portal builds absolute URLs from it: 8888 for offline, 8889 for online.

The marketplace volume is ${_MARKETPLACE_DIR} on this host, one
subdirectory per namespace.

After rebuilding the bundles, pick them up with:
    bash ${_SCRIPT_DIR}/build_license_activation_jars.sh ${_JARS_DIR}
    kubectl --namespace <namespace> rollout restart statefulset liferay-default

Tear the cluster down with:
    k3d cluster delete ${_CLUSTER_NAME}
================================================================
EOF
}

_SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

_ARGO_CHART_VERSION="2.0.3"

_ARGO_NAMESPACE="argo-workflows-system"

_CLOUD_DIR=$(cd "${_SCRIPT_DIR}/../.." && pwd)

_CLUSTER_NAME="offline-activation-poc"

_JARS_DIR="${_SCRIPT_DIR}/jars"

_OFFLINE_NAMESPACE="liferay-offline"

_ONLINE_NAMESPACE="liferay-online"

_MARKETPLACE_DIR="${_SCRIPT_DIR}/marketplace"

_OPERATOR_IMAGE="liferay-dxp-operator-poc:latest"

_OPERATOR_NAMESPACE="dxp-operator-system"

_PROVISIONING_BASE_URL="https://api.one-uat.liferay.com"

main "${@}"