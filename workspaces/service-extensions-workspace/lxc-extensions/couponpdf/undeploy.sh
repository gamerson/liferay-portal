#!/usr/bin/env bash

kubectl config use-context kind-kind
kubectl config set-context --current --namespace=default

kapp \
  delete \
  -a couponpdf \
  -y