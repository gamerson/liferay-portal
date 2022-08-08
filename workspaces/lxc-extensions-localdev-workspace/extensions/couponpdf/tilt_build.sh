#!/usr/bin/env bash

set -e

cd extensions/couponpdf

mvn clean package

cp ../../k8s/tls/ca.crt .

docker build -t $EXPECTED_REF .