#!/bin/bash
#
# Usage:
#   scripts/run.sh <peer_id>
#
# Compiles sources to build/classes and launches a single peer.

set -e

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <peer_id>"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BUILD_DIR="${REPO_ROOT}/build/classes"

mkdir -p "${BUILD_DIR}"
javac -d "${BUILD_DIR}" "${REPO_ROOT}"/src/*.java

cd "${REPO_ROOT}"
java -cp "${BUILD_DIR}" peerProcess "$1"
