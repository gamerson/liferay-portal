#!/usr/bin/env bash

k3d cluster create --config k3d/config.yaml --registry-create registry.localdev.me:5000

kubectl create -f k3d/rbac.yaml