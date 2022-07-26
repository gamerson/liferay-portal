#!/usr/bin/env bash

#lcp deploy -r liferayperf.sh -p cosettepoc-dev

mvn clean package

docker build -t localhost:5001/couponpdf:latest .

docker push localhost:5001/couponpdf:latest

# install kapp
# install ytt

kubectl config use-context kind-kind
kubectl config set-context --current --namespace=default

kapp \
  deploy \
  -a couponpdf \
  -y \
  -f <(ytt \
        -f ../../k8s/microservice \
        -f configurator/couponpdf.client-extension-config.json \
        --data-value serviceId=couponpdf \
        --data-value image=localhost:5001/couponpdf:latest)