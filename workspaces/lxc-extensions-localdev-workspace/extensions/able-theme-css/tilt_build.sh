#!/usr/bin/env bash

set -e

./gradlew :extensions:able-theme-css:clean :extensions:able-theme-css:build

cd extensions/able-theme-css/dist

unzip able-theme-css.zip

docker build -t $EXPECTED_REF .