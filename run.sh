#!/bin/bash

set -e

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <peer_id>"
    exit 1
fi

javac ./*.java
java peerProcess "$1"
