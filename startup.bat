@echo off
start powershell.exe -NoExit -Command "cd .\BitTorrent-Clone\; java peerProcess 1001"
start powershell.exe -NoExit -Command "cd .\BitTorrent-Clone\; java peerProcess 1002"
start powershell.exe -NoExit -Command "cd .\BitTorrent-Clone\; java peerProcess 1003"