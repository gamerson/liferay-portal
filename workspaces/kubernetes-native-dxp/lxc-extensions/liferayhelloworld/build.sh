#!/bin/bash

cd "$(dirname "$0")"

eval $(minikube docker-env)

echo "[run_local] Build the liferayhelloworld PoC"

yarn install && yarn build-local &&\
  docker build -t $IMAGE .