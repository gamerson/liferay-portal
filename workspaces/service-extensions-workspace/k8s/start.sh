#!/usr/bin/env bash

kind create cluster --config=./kind/cluster.yaml --image kindest/node:v1.20.15

kubectl config use-context kind-kind
kubectl config set-context --current --namespace=default

kubectl create -f ./kind/rbac.yaml

DEFAULT_TOKEN=$(kubectl get secret --namespace default | grep 'default-token' | awk '{print $1}')

echo "KUBERNETES_CERTIFICATE=$(kubectl get secret $DEFAULT_TOKEN -o jsonpath={.data.'ca\.crt'} | base64 -d)" > .certificate
echo "KUBERNETES_TOKEN=$(kubectl get secret $DEFAULT_TOKEN -o jsonpath={.data.'token'} | base64 -d)" > .token