#!/bin/bash

for i in 1 2 3; do
    xterm -e "bash -c 'cd ./BitTorrent-Clone && java peerProcess 100$i; exec bash'" &
done

wait