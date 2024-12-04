#!/bin/bash

set -ex

script_dir=$(dirname "$0")

docker run \
  -it \
  --network bridge-cx \
  -p 80:80 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v ${script_dir}/static.yml:/etc/traefik/traefik.yml \
  -v ${script_dir}/configs/:/traefik/configs/ \
  traefik:v3.2
