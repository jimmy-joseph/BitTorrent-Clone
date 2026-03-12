# BitTorrent Clone — CNT4007

A Java implementation of the BitTorrent peer-to-peer file sharing protocol.

---

## Requirements

- Java JDK 8 or higher
---

## Startup

### 1. Compile

From the repository root:

```bash
javac *.java
```

### 2. Launch Peers

**Windows** — opens each peer in a separate PowerShell window:

```bat
.\startup.bat
```

**Linux / WSL:**

```bash
bash startup.sh
```

Both scripts launch peers 1001, 1002, and 1003. To run a single peer manually:

```bash
java peerProcess <peerId>
```

Peers must be started in the order they appear in `PeerInfo.cfg`. Each peer connects to all peers listed before it and listens for connections from peers listed after it.

---

## File Structure

```
BitTorrent-Clone/
├── peerProcess.java       # Main entry point — initializes a peer and manages connections
├── peerConnection.java    # Handles a single TCP connection to/from another peer
├── peerState.java         # Tracks per-peer state (choked, interested, bitfield, rate)
├── Message.java           # Message type constants and serialization
├── Handshake.java         # 32-byte handshake protocol
├── Logger.java            # Timestamped logging to file and console
├── Parser.java            # Parses Common.cfg into a Properties object
├── NeighborManager.java   # Choking/unchoking algorithm (in progress)
│
├── Common.cfg             # Global protocol settings (piece size, intervals, etc.)
├── PeerInfo.cfg           # List of all peers (ID, host, port, has-file flag)
│
├── startup.bat            # Windows launch script (peers 1001–1003)
├── startup.sh             # Linux/WSL launch script (peers 1001–1003)
│
└── peers/
    ├── 1001/thefile       # Shared file for peer 1001 (seed)
    └── 1006/thefile       # Shared file for peer 1006 (seed)
```

## Protocol Overview

1. Each peer reads `Common.cfg` and `PeerInfo.cfg` on startup.
2. A peer connects (outgoing TCP) to every peer listed before it in `PeerInfo.cfg`.
3. A peer listens for incoming connections from peers listed after it.
4. Both sides exchange a handshake.
5. Peers with at least one piece send a `BITFIELD` message advertising what they have.
6. Peers reply with `INTERESTED` or `NOT_INTERESTED`.