#!/usr/bin/env bash

k3d cluster create \
  --config k3d/config.yaml \
  --registry-create registry.localdev.me:5000 \
  --wait

kubectl create secret generic localdev-tls-secret \
  --from-file=tls.crt=./tls/localdev.me.crt \
  --from-file=tls.key=./tls/localdev.me.key  \
  --namespace default