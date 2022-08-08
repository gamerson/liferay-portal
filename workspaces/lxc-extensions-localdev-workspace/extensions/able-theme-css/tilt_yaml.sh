#!/usr/bin/env bash

set -e

./gradlew clean createClientExtensionConfig > /dev/null 2> /dev/null

ytt -f k8s/extension -f extensions/able-theme-css/build/able-theme-css.client-extension-config.json --data-value image=able-theme-css --data-value serviceId=able-theme-css