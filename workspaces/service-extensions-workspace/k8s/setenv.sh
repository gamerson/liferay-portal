#!/usr/bin/env bash

kubectl config use-context kind-kind
kubectl config set-context --current --namespace=default

DEFAULT_TOKEN=$(kubectl get secret --namespace default | grep 'default-token' | awk '{print $1}')

DIR=$(dirname $0)

kubectl get secret $DEFAULT_TOKEN -o jsonpath={.data.'ca\.crt'} | base64 -d > "${DIR}"/.certificate
kubectl get secret $DEFAULT_TOKEN -o jsonpath={.data.'token'} | base64 -d > "${DIR}"/.token