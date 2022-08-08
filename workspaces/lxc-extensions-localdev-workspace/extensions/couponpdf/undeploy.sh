#!/usr/bin/env bash

kubectl config use-context k3d-lxc-localdev
kubectl config set-context --current --namespace=default

kapp \
  delete \
  -a couponpdf \
  -y