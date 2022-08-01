#!/usr/bin/env bash

(cd ..; ./gradlew clean buildDockerImage)

docker tag service-extensions-dxp:latest registry.localdev.me:5000/service-extensions-dxp:latest
docker push registry.localdev.me:5000/service-extensions-dxp:latest

kubectl config use-context k3d-lxc-localdev
kubectl config set-context --current --namespace=default

kapp \
  deploy \
  -a dxp \
  -y \
  -f <(ytt \
        -f dxp \
        --data-value image=registry.localdev.me:5000/service-extensions-dxp:latest)