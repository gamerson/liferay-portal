#!/usr/bin/env bash

#mvn clean package

#lcp deploy -r liferayperf.sh -p cosettepoc-dev

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