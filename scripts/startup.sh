#!/bin/bash
#
# Usage:
#   scripts/startup.sh            # launches peers 1001 1002 1003 in the background
#   scripts/startup.sh 1001 1002  # launches just the specified peer IDs
#
# Each peer's stdout/stderr is written to logs/peer_<id>.out.
# Structured log lines still go to logs/log_peer_<id>.log.
#
# The script starts peers in the order given and sleeps briefly between
# each one, so earlier peers are ready to accept connections before later
# peers try to dial in.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BUILD_DIR="${REPO_ROOT}/build/classes"
LOG_DIR="${REPO_ROOT}/logs"

mkdir -p "${BUILD_DIR}" "${LOG_DIR}"

echo "Compiling..."
javac -d "${BUILD_DIR}" "${REPO_ROOT}"/src/*.java

if [ "$#" -eq 0 ]; then
    PEERS=(1001 1002 1003 1004 1005 1006)
else
    PEERS=("$@")
fi

pids=()
for id in "${PEERS[@]}"; do
    echo "Starting peer ${id}..."
    (
        cd "${REPO_ROOT}"
        java -cp "${BUILD_DIR}" peerProcess "${id}" \
            >"${LOG_DIR}/peer_${id}.out" 2>&1
    ) &
    pids+=($!)
    sleep 1
done

echo "Launched peers: ${PEERS[*]}"
echo "PIDs: ${pids[*]}"
echo "Tail logs with: tail -f logs/log_peer_*.log"
echo "Waiting for all peers to finish..."

for pid in "${pids[@]}"; do
    wait "${pid}" || true
done

echo "All peers exited."
