#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BUILD_DIR="${REPO_ROOT}/build/classes"

mkdir -p "${BUILD_DIR}"
javac -d "${BUILD_DIR}" "${REPO_ROOT}"/src/*.java

for i in 1 2 3; do
    xterm -e "bash -c 'cd \"${REPO_ROOT}\" && java -cp \"${BUILD_DIR}\" peerProcess 100$i; exec bash'" &
done

wait
