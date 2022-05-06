#!/bin/bash

cd "$(dirname "$0")"

mkdir -p build/libs

bnd buildx -o build/libs bnd.bnd