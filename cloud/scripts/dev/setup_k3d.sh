#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

_ARGOCD_CHART_VERSION="9.5.16"

_ARGOCD_NAMESPACE="argocd"

_CLUSTER_NAME=${LIFERAY_K3D_CLUSTER_NAME:-cne}

_ENVIRONMENT_NAME="liferay-default"

_ENVIRONMENT_NAMESPACE="liferay-dev"

_HARNESS_NAMESPACE="liferay-cne"

_K3S_IMAGE="rancher/k3s:v1.31.4-k3s1"

_OPERATOR_IMAGE_REPOSITORY="liferay/liferay-dxp-operator"

_OPERATOR_IMAGE_TAG="dev"

_OPERATOR_NAMESPACE="liferay-system"

_PROVISIONING_MOCK_IMAGE_REPOSITORY="liferay/provisioning-mock"

_PROVISIONING_MOCK_IMAGE_TAG="dev"

_REPOSITORY_URL="git://gitserver.liferay-cne.svc.cluster.local:9418/environment.git"

_DEV_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

_ROOT_CLOUD_DIR=$(cd "${_DEV_DIR}/../.." && pwd)

function main {
	if [[ "${BASH_SOURCE[0]}" != "${0}" ]]
	then
		return
	fi

	local command=${1:-}

	case "${command}" in
		down)
			_check_utils k3d

			_delete_cluster
			;;
		seed)
			_check_utils git kubectl

			_seed_git_repository
			;;
		status)
			_check_utils kubectl

			_print_status
			;;
		up)
			_check_utils docker git go helm java jq k3d kubectl

			_create_cluster

			_install_argocd

			_install_git_server

			_seed_git_repository

			_install_provisioning_mock

			_install_operator

			_create_environment

			_print_status

			_print_next_steps
			;;
		*)
			_print_usage

			exit 1
			;;
	esac
}

function _argocd_values {
	cat << EOF
configs:
    cm:
        application.resourceTrackingMethod: annotation
        timeout.reconciliation: 20s
    params:
        server.insecure: "true"
controller:
    resources:
        requests:
            cpu: 50m
            memory: 256Mi
dex:
    enabled: false
notifications:
    enabled: false
redis:
    resources:
        requests:
            cpu: 10m
            memory: 64Mi
repoServer:
    resources:
        requests:
            cpu: 20m
            memory: 128Mi
server:
    resources:
        requests:
            cpu: 20m
            memory: 128Mi
EOF
}

function _build_operator_image {
	echo "Building the operator image ${_OPERATOR_IMAGE_REPOSITORY}:${_OPERATOR_IMAGE_TAG} from the working tree."

	(
		cd "${_ROOT_CLOUD_DIR}/operator"

		./go_build.sh fast

		docker build \
			--file - \
			--quiet \
			--tag "${_OPERATOR_IMAGE_REPOSITORY}:${_OPERATOR_IMAGE_TAG}" \
			resources/build << EOF
FROM gcr.io/distroless/static:nonroot
COPY manager /manager
USER 65532:65532
ENTRYPOINT ["/manager"]
EOF
	)

	k3d image import "${_OPERATOR_IMAGE_REPOSITORY}:${_OPERATOR_IMAGE_TAG}" --cluster "${_CLUSTER_NAME}"
}

function _build_provisioning_mock_image {
	echo "Building the provisioning mock image ${_PROVISIONING_MOCK_IMAGE_REPOSITORY}:${_PROVISIONING_MOCK_IMAGE_TAG} from the working tree."

	(
		cd "${_ROOT_CLOUD_DIR}/operator/resources"

		mkdir --parents build

		CGO_ENABLED=0 GOOS=linux go build -o build/provisioning-mock ./dev/provisioning-mock

		docker build \
			--file - \
			--quiet \
			--tag "${_PROVISIONING_MOCK_IMAGE_REPOSITORY}:${_PROVISIONING_MOCK_IMAGE_TAG}" \
			build << EOF
FROM alpine:3
COPY provisioning-mock /provisioning-mock
ENTRYPOINT ["/provisioning-mock"]
EOF
	)

	k3d image import "${_PROVISIONING_MOCK_IMAGE_REPOSITORY}:${_PROVISIONING_MOCK_IMAGE_TAG}" --cluster "${_CLUSTER_NAME}"
}

function _check_utils {
	for util in "${@}"
	do
		if ! command -v "${util}" &> /dev/null
		then
			echo "The utility ${util} is not installed." >&2

			exit 1
		fi
	done
}

function _create_cluster {
	if ! k3d cluster list "${_CLUSTER_NAME}" &> /dev/null
	then
		echo "Creating the k3d cluster ${_CLUSTER_NAME}."

		k3d cluster create "${_CLUSTER_NAME}" \
			--image "${_K3S_IMAGE}" \
			--kubeconfig-switch-context=false \
			--wait

		return
	fi

	local servers_running=$(k3d cluster list "${_CLUSTER_NAME}" --output json | jq --raw-output ".[0].serversRunning")

	if [[ ${servers_running} -gt 0 ]]
	then
		echo "The k3d cluster ${_CLUSTER_NAME} is already running."

		return
	fi

	echo "Starting the k3d cluster ${_CLUSTER_NAME}."

	k3d node delete "k3d-${_CLUSTER_NAME}-tools" &> /dev/null || true

	k3d cluster start "${_CLUSTER_NAME}"
}

function _create_environment {
	echo "Creating the ArgoCD application that owns the environment."

	_kubectl apply --filename - << EOF
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
    name: ${_ENVIRONMENT_NAMESPACE}
    namespace: ${_ARGOCD_NAMESPACE}
spec:
    destination:
        namespace: ${_ENVIRONMENT_NAMESPACE}
        server: https://kubernetes.default.svc
    project: default
    source:
        helm:
            valueFiles:
                -   values-k3d.yaml
        path: chart
        repoURL: ${_REPOSITORY_URL}
        targetRevision: main
    syncPolicy:
        automated:
            prune: true
            selfHeal: true
        syncOptions:
            -   CreateNamespace=true
            -   RespectIgnoreDifferences=true
EOF

	_wait_for "the environment namespace" \
		_kubectl get namespace "${_ENVIRONMENT_NAMESPACE}"

	echo "Creating the activation code secret, which the chart references but never creates."

	_kubectl \
		create secret generic "${_ENVIRONMENT_NAME}-activation" \
		--dry-run=client \
		--from-literal=activationCode=k3d-activation-code \
		--namespace "${_ENVIRONMENT_NAMESPACE}" \
		--output yaml | _kubectl apply --filename -

	_wait_for "the environment to activate" \
		_environment_is_ready
}

function _delete_cluster {
	if ! k3d cluster list "${_CLUSTER_NAME}" &> /dev/null
	then
		echo "The k3d cluster ${_CLUSTER_NAME} does not exist."

		return
	fi

	k3d cluster delete "${_CLUSTER_NAME}"
}

function _environment_is_ready {
	local phase

	phase=$(_kubectl get liferayenvironment "${_ENVIRONMENT_NAME}" --namespace "${_ENVIRONMENT_NAMESPACE}" --output jsonpath="{.status.phase}" 2> /dev/null)

	[[ ${phase} == "Ready" ]]
}

function _gitserver_pod {
	_kubectl \
		get pod \
		--field-selector status.phase=Running \
		--namespace "${_HARNESS_NAMESPACE}" \
		--output jsonpath="{.items[0].metadata.name}" \
		--selector app=gitserver
}

function _install_argocd {
	echo "Installing ArgoCD ${_ARGOCD_CHART_VERSION}, the version the AWS platform stage pins."

	helm repo add argo https://argoproj.github.io/argo-helm --force-update &> /dev/null

	helm repo update argo &> /dev/null

	_argocd_values | helm upgrade \
		--create-namespace \
		--install argocd argo/argo-cd \
		--kube-context "k3d-${_CLUSTER_NAME}" \
		--namespace "${_ARGOCD_NAMESPACE}" \
		--timeout 10m \
		--values - \
		--version "${_ARGOCD_CHART_VERSION}" \
		--wait
}

function _install_git_server {
	echo "Installing the in-cluster git server that stands in for the GitOps repository."

	_kubectl create namespace "${_HARNESS_NAMESPACE}" --dry-run=client --output yaml | _kubectl apply --filename -

	_kubectl apply --filename - << EOF
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
    name: gitserver
    namespace: ${_HARNESS_NAMESPACE}
spec:
    accessModes:
        -   ReadWriteOnce
    resources:
        requests:
            storage: 1Gi
    storageClassName: local-path
---
apiVersion: apps/v1
kind: Deployment
metadata:
    name: gitserver
    namespace: ${_HARNESS_NAMESPACE}
spec:
    replicas: 1
    selector:
        matchLabels:
            app: gitserver
    template:
        metadata:
            labels:
                app: gitserver
        spec:
            containers:
                -   command:
                        -   sh
                        -   -c
                        -   |
                            set -e

                            apk add --no-cache git-daemon

                            exec git daemon \
                                --base-path=/srv/git \
                                --enable=receive-pack \
                                --export-all \
                                --reuseaddr \
                                --verbose
                    image: alpine/git:latest
                    name: gitserver
                    ports:
                        -   containerPort: 9418
                            name: git
                    volumeMounts:
                        -   mountPath: /srv/git
                            name: repositories
            volumes:
                -   name: repositories
                    persistentVolumeClaim:
                        claimName: gitserver
---
apiVersion: v1
kind: Service
metadata:
    name: gitserver
    namespace: ${_HARNESS_NAMESPACE}
spec:
    ports:
        -   name: git
            port: 9418
            targetPort: git
    selector:
        app: gitserver
EOF

	_kubectl --namespace "${_HARNESS_NAMESPACE}" rollout status deploy/gitserver --timeout=5m
}

function _install_operator {
	_build_operator_image

	echo "Installing the operator chart, which also installs the CRD and the admission policy."

	_kubectl create namespace "${_OPERATOR_NAMESPACE}" --dry-run=client --output yaml | _kubectl apply --filename -

	_kubectl apply --filename - << EOF
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
    name: marketplace
    namespace: ${_OPERATOR_NAMESPACE}
spec:
    accessModes:
        -   ReadWriteOnce
    resources:
        requests:
            storage: 1Gi
    storageClassName: local-path
EOF

	helm upgrade \
		--install liferay-dxp-operator "${_ROOT_CLOUD_DIR}/helm/dxp-operator" \
		--kube-context "k3d-${_CLUSTER_NAME}" \
		--namespace "${_OPERATOR_NAMESPACE}" \
		--set debug=true \
		--set gracePeriod=10m \
		--set heartbeatInterval=30s \
		--set image.pullPolicy=Never \
		--set "image.repository=${_OPERATOR_IMAGE_REPOSITORY}" \
		--set "image.tag=${_OPERATOR_IMAGE_TAG}" \
		--set marketplace.claimName=marketplace \
		--set marketplace.enabled=true \
		--set podSecurityContext.fsGroup=65532 \
		--set "provisioning.baseURL=http://provisioning-mock.${_HARNESS_NAMESPACE}.svc.cluster.local:8080" \
		--set retry.maxDelay=1m \
		--timeout 5m \
		--wait
}

function _install_provisioning_mock {
	_build_provisioning_mock_image

	echo "Installing the provisioning mock that issues the license."

	_kubectl apply --filename - << EOF
apiVersion: apps/v1
kind: Deployment
metadata:
    name: provisioning-mock
    namespace: ${_HARNESS_NAMESPACE}
spec:
    replicas: 1
    selector:
        matchLabels:
            app: provisioning-mock
    template:
        metadata:
            labels:
                app: provisioning-mock
        spec:
            containers:
                -   image: ${_PROVISIONING_MOCK_IMAGE_REPOSITORY}:${_PROVISIONING_MOCK_IMAGE_TAG}
                    imagePullPolicy: Never
                    name: provisioning-mock
                    ports:
                        -   containerPort: 8080
                            name: http
---
apiVersion: v1
kind: Service
metadata:
    name: provisioning-mock
    namespace: ${_HARNESS_NAMESPACE}
spec:
    ports:
        -   name: http
            port: 8080
            targetPort: http
    selector:
        app: provisioning-mock
EOF

	_kubectl --namespace "${_HARNESS_NAMESPACE}" rollout restart deploy/provisioning-mock

	_kubectl --namespace "${_HARNESS_NAMESPACE}" rollout status deploy/provisioning-mock --timeout=5m
}

function _kubectl {
	kubectl --context "k3d-${_CLUSTER_NAME}" "${@}"
}

function _print_next_steps {
	local password=$(_kubectl get secret argocd-initial-admin-secret --namespace "${_ARGOCD_NAMESPACE}" --output jsonpath="{.data.password}" | base64 --decode)

	echo ""
	echo "The cluster is ready. Drive the licensing scenarios with the regression suite."
	echo ""
	echo "    ${_ROOT_CLOUD_DIR}/scripts/tests/licensing_conflicts_test.sh"
	echo ""
	echo "Open the ArgoCD console with the following, then log in as admin."
	echo ""
	echo "    kubectl --context k3d-${_CLUSTER_NAME} --namespace ${_ARGOCD_NAMESPACE} port-forward svc/argocd-server 8090:80"
	echo ""
	echo "    Password: ${password}"
	echo ""
	echo "Run the provisioning mock on the host instead, and point the operator at it."
	echo ""
	echo "    (cd ${_ROOT_CLOUD_DIR}/operator/resources && go run ./dev/provisioning-mock)"
	echo "    helm upgrade liferay-dxp-operator ${_ROOT_CLOUD_DIR}/helm/dxp-operator --namespace ${_OPERATOR_NAMESPACE} --reuse-values --set provisioning.baseURL=http://host.k3d.internal:8080"
	echo ""
	echo "Reseed the GitOps repository from the working tree, or tear the cluster down."
	echo ""
	echo "    ${0} seed"
	echo "    ${0} down"
	echo ""
	echo "Two things are expected rather than broken. The LiferayEnvironment stays"
	echo "OutOfSync, because the chart renders spec.marketplaceVolume and the CRD no"
	echo "longer declares it, so the API server prunes the field on every sync. The"
	echo "workload also runs a pause image rather than DXP, since a replica conflict"
	echo "only needs the StatefulSet write to be admitted or denied."
	echo ""
	echo "This script installs the operator itself, so do not run tilt up against the"
	echo "same cluster. Use cloud/operator/tilt_up.sh on its own cluster instead."
}

function _print_status {
	echo ""
	echo "LiferayEnvironment"

	_kubectl --namespace "${_ENVIRONMENT_NAMESPACE}" get liferayenvironment --output wide ||
		true

	echo ""
	echo "Workload"

	_kubectl --namespace "${_ENVIRONMENT_NAMESPACE}" get statefulset,pods || true

	echo ""
	echo "ArgoCD application"

	_kubectl get application "${_ENVIRONMENT_NAMESPACE}" --namespace "${_ARGOCD_NAMESPACE}" --output jsonpath="{.status.sync.status}{\"\t\"}{.status.operationState.phase}{\"\n\"}{.status.operationState.message}{\"\n\"}" || true

	echo ""
	echo "Operator service account exemption"

	local match_conditions

	match_conditions=$(_kubectl get validatingadmissionpolicy liferay-dxp-operator-statefulset-scale --output jsonpath="{.spec.matchConditions[*].name}" 2> /dev/null)

	if [[ ${match_conditions} == *"exclude-operator-serviceaccount"* ]]
	then
		echo "Present. The operator's own replica writes bypass the policy."
	else
		echo "Absent. The operator's own replica writes are validated against the policy."
	fi
}

function _print_usage {
	echo "Usage: ${0} <command>" >&2
	echo "" >&2
	echo "Commands:" >&2
	echo "    down    Delete the k3d cluster" >&2
	echo "    seed    Push the working tree's chart to the GitOps repository" >&2
	echo "    status  Report the environment, workload, and sync state" >&2
	echo "    up      Create the cluster and everything in it" >&2
	echo "" >&2
	echo "Environment variables:" >&2
	echo "    LIFERAY_K3D_CLUSTER_NAME  The k3d cluster name, ${_CLUSTER_NAME} by default" >&2
}

function _seed_git_repository {
	echo "Pushing the working tree's default chart to the GitOps repository."

	local temporary_dir

	temporary_dir=$(mktemp --directory)

	cp --recursive "${_ROOT_CLOUD_DIR}/helm/default" "${temporary_dir}/chart"

	_values_k3d > "${temporary_dir}/chart/values-k3d.yaml"

	local pod=$(_gitserver_pod)

	_kubectl --namespace "${_HARNESS_NAMESPACE}" exec "${pod}" -- sh -c '
		if [ ! -d /srv/git/environment.git ]
		then
			git init --bare --initial-branch=main --quiet /srv/git/environment.git
		fi

		rm -rf /srv/git/seed && mkdir -p /srv/git/seed'

	_kubectl cp "${temporary_dir}/." "${_HARNESS_NAMESPACE}/${pod}:/srv/git/seed"

	rm --force --recursive "${temporary_dir}"

	_kubectl --namespace "${_HARNESS_NAMESPACE}" exec "${pod}" -- sh -c '
		cd /srv/git/seed

		git init --initial-branch=main --quiet
		git config user.email k3d@liferay.com
		git config user.name "Liferay k3d"
		git add --all
		git commit --message "Seed the environment" --quiet
		git push --force --quiet /srv/git/environment.git main'
}

function _values_k3d {
	cat << 'EOF'
customInitContainers:
    x-liferay-prepopulate-data: null
    x-liferay-wait-on-services: null
image:
    repository: registry.k8s.io/pause
    tag: "3.9"
livenessProbe: null
readinessProbe: null
replicaCount: 3
resources:
    limits:
        cpu: 100m
        memory: 64Mi
    requests:
        cpu: 10m
        memory: 32Mi
startupProbe: null
EOF
}

function _wait_for {
	local description=${1}

	shift

	echo "Waiting for ${description}."

	local attempt=0

	while [[ ${attempt} -lt 60 ]]
	do
		if "${@}" &> /dev/null
		then
			return
		fi

		attempt=$((attempt + 1))

		sleep 5
	done

	echo "Timed out waiting for ${description}." >&2

	exit 1
}

main "${@}"