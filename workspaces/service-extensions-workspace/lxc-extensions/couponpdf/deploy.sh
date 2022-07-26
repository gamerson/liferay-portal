#!/usr/bin/env bash

#lcp deploy -r liferayperf.sh -p cosettepoc-dev

mvn clean package

docker build -t couponpdf .

# install kapp
# install ytt

kapp \
  deploy \
  -a couponpdf \
  -f <(ytt \
        -f ../../k8s/microservice \
        -f configurator/couponpdf.service-extension-config.json \
        --data-value serviceId=couponpdf \
        --data-value image=couponpdf)