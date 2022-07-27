#!/usr/bin/env bash

#k3d registry create lxc-registry.localdev.me --port 5000
k3d cluster create --config k3d/config.yaml --registry-create lxc-registry.localdev.me:5000

kubectl create -f k3d/rbac.yaml