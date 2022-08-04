#!/usr/bin/env bash

set -e

IMAGE="registry.localdev.me:5000/service-extensions-dxp:latest"

(cd ..; ./gradlew clean buildDockerImage -Pdocker.image.id=${IMAGE})

docker push ${IMAGE}

kubectl config use-context k3d-lxc-localdev
kubectl config set-context --current --namespace=default

kapp \
  deploy \
  -a dxp \
  -f <(ytt \
        -f dxp \
        --data-value image=${IMAGE}) \
  -y