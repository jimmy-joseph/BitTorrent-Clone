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
javac -d build/classes src/*.java
```

### 2. Launch Peers

**Windows** — opens each peer in a separate PowerShell window:

```bat
.\scripts\startup.bat
```

**Linux / WSL:**

```bash
bash scripts/startup.sh
```

Both scripts launch peers 1001, 1002, and 1003. To run a single peer manually:

```bash
java -cp build/classes peerProcess <peerId>
```

Peers must be started in the order they appear in `config/PeerInfo.cfg`. Each peer connects to all peers listed before it and listens for connections from peers listed after it.

---

## File Structure

```
BitTorrent-Clone/
├── config/
│   ├── Common.cfg         # Global protocol settings
│   └── PeerInfo.cfg       # Peer IDs, hosts, ports, and seed flags
├── scripts/
│   ├── startup.bat        # Windows launcher
│   └── startup.sh         # Linux / WSL launcher
├── src/
│   ├── peerProcess.java   # Main entry point
│   ├── peerConnection.java
│   ├── peerState.java
│   ├── Message.java
│   ├── Handshake.java
│   ├── Logger.java
│   ├── Parser.java
│   └── NeighborManager.java
├── sample-data/
│   └── peers/
│       ├── 1001/thefile   # Example seeded file
│       └── 1006/thefile   # Example seeded file
└── archive/
    └── proj1.tar          # Legacy project submission bundle
```

Generated output is intentionally kept out of the root:

- `build/` contains compiled `.class` files
- `logs/` contains runtime peer logs

## Protocol Overview

1. Each peer reads `config/Common.cfg` and `config/PeerInfo.cfg` on startup.
2. A peer connects (outgoing TCP) to every peer listed before it in `config/PeerInfo.cfg`.
3. A peer listens for incoming connections from peers listed after it.
4. Both sides exchange a handshake.
5. Peers with at least one piece send a `BITFIELD` message advertising what they have.
6. Peers reply with `INTERESTED` or `NOT_INTERESTED`.
