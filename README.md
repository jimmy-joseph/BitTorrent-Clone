CNT4007 Project - BitTorrent Clone

Group 76 Members:
Jimmy Joseph, Shelton Joseph, Fabian Ho Chang

To launch the respective peer id's run the command: 

(Linux)
./scripts/run.sh <peerId>
Ex: ./scripts/run.sh 1001
Repeat for all peerId's

(Windows)
./scripts/run.bat <peerId>
Ex: ./scripts/run.bat 1001
Repeat for all peerId's

To compile manually and run run:
javac -d build/classes src/*.java
java -cp build/classes peerProcess <peerId>

This is our implementation of the CNT Project, we used classes to represent individual peers.
Our project can be used to upload any file, you just have to change the config file Common.cfg and PeerInfo.cfg files which have the file data and peer data respectively.

Fun fact: We sent our demo video to each group member using our own bittorrent protocol!