#!/usr/bin/env bash

set -e

mvn clean package

cp ../../k8s/tls/ca.crt .

SERVICE="couponpdf"
IMAGE="registry.localdev.me:5000/${SERVICE}:latest"

docker build -t $IMAGE .
docker push $IMAGE

kubectl config use-context k3d-lxc-localdev
kubectl config set-context --current --namespace=default

kapp \
  deploy \
  -a $SERVICE \
  -y \
  -f <(ytt \
        -f ../../k8s/extension \
        -f configurator/${SERVICE}.client-extension-config.json \
        --data-value cpu=500m \
        --data-value image=$IMAGE \
        --data-value memory=512Mi \
        --data-value serviceId=${SERVICE} \
        --data-value-yaml initMetadata=true)