#!/usr/bin/env bash

set -e

kubectl config use-context k3d-lxc-localdev
kubectl config set-context --current --namespace=default

kapp \
  delete \
  -a able-theme-css \
  -y