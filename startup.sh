#!/bin/bash

for i in 1 2 3; do
    powershell.exe -NoExit -Command "cd .\BitTorrent-Clone\; java peerProcess 100$i" &
done

wait