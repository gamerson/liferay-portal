#!/bin/bash

set -eux

root_cloud_dir="$(dirname "$0")/.."

INPUT_FILE="${1:-}"
TFVARS_ARG=""

if [[ "$INPUT_FILE" == *.tfvars ]]; then
	TFVARS_ARG="-var-file=$(realpath "$INPUT_FILE")"
fi

function log_message {
	echo "$1" >&2
}

function main {
	log_message "Attempting to login to your AWS account via SSO..."

	aws sso login

	local deployment_info=$(setup_aws_eks)

	echo $deployment_info

	local deployment_name
	local region
	read -r deployment_name region <<< "${deployment_info}"

	setup_aws_gitops "${deployment_name}" "${region}"

	port_forward_argocd
}

function port_forward_argocd {
	local argocd_password=$(kubectl get secret argocd-initial-admin-secret --namespace argocd --output jsonpath="{.data.password}" | base64 --decode)

	log_message "Port-forwarding the ArgoCD service at localhost:8080...."
	log_message "Login with Username: admin and Password: ${argocd_password} for further setup monitoring."
	log_message "Use CTRL+C to exit when finished."

	kubectl port-forward --namespace argocd service/argocd-server 8080:443
}

function setup_aws_eks {
	_pushd "${root_cloud_dir}/terraform/aws/eks"

	log_message "Setting up the AWS EKS cluster..."

	terraform_init_and_apply "." ${TFVARS_ARG}

	local deployment_name=$(terraform output -raw deployment_name)
	local region=$(terraform output -raw region)
	local cluster_name=$(terraform output -raw cluster_name)

	aws eks update-kubeconfig --name "${cluster_name}" --region "${region}" >&2

	export KUBE_CONFIG_PATH="${HOME}/.kube/config"

	log_message "AWS EKS cluster setup complete."

	echo "${deployment_name} ${region}"

	_popd
}

function setup_aws_gitops {
	local deployment_name="$1"
	local region="$2"

	_pushd "${root_cloud_dir}/terraform/aws/gitops"

	log_message "Setting up GitOps Infrastructure..."

	terraform_init_and_apply "./platform" ${TFVARS_ARG}

	terraform_init_and_apply "./resources" "-var=deployment_name=${deployment_name}" "-var=region=${region}" ${TFVARS_ARG}

	log_message "GitOps Infrastructure setup complete."

	_popd
}

function terraform_init_and_apply {
	_pushd "$1"
	shift

	terraform init 

	terraform apply "$@"

	_popd
}

function _popd {
	popd > /dev/null
}

function _pushd {
	pushd "$1" > /dev/null
}

main