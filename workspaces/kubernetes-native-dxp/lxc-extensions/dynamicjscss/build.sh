#!/bin/bash

cd "$(dirname "$0")"

eval $(minikube docker-env)

if [[ ! -z "${EXPECTED_REF}" ]]; then
  IMAGE=${EXPECTED_REF}
fi

ID=dynamicjscss

echo "[run_local] Build the $ID PoC"

cat << EOF > ../../k8s/$ID/extension-configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: $ID-dxp-configs
  labels:
    cloud.liferay.com/serviceId: $ID
    dxp.liferay.com/configs: "true"
  annotations:
    cloud.liferay.com/context-data: '{"domains":["$ID.localdev.me"]}'
data:
  osgi.config.json: |
EOF
sed -e 's/^/    /' configurator/osgi.config.json \
	>> ../../k8s/$ID/extension-configmap.yaml

./assemble.sh

unzip build/libs/*.jar -d build/unzip

cd build/unzip
docker build -t $IMAGE .
