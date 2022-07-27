#!/usr/bin/env bash

#lcp deploy -r liferayperf.sh -p cosettepoc-dev

mvn clean package

docker build -t lxc-registry.localdev.me:5000/couponpdf:latest .
docker push lxc-registry.localdev.me:5000/couponpdf:latest

# install kapp
# install ytt

kubectl config use-context k3d-lxc-extensions
kubectl config set-context --current --namespace=default

kapp \
  deploy \
  -a couponpdf \
  -y \
  -f <(ytt \
        -f ../../k8s/microservice \
        -f configurator/couponpdf.client-extension-config.json \
        --data-value serviceId=couponpdf \
        --data-value image=lxc-registry.localdev.me:5000/couponpdf:latest)