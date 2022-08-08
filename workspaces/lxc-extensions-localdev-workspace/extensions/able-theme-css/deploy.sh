#!/usr/bin/env bash

set -e

../../gradlew clean build

SERVICE="able-theme-css"
IMAGE="registry.localdev.me:5000/${SERVICE}:latest"

(cd dist; unzip ${SERVICE}.zip; docker build -t $IMAGE .)

docker push $IMAGE

kubectl config use-context k3d-lxc-localdev
kubectl config set-context --current --namespace=default

kapp \
  deploy \
  -a $SERVICE \
  -y \
  -f <(ytt \
        -f ../../k8s/extension \
        -f dist/${SERVICE}.client-extension-config.json \
        --data-value image=${IMAGE} \
        --data-value serviceId=${SERVICE})