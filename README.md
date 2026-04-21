# BitTorrent Clone — CNT4007

A Java implementation of a BitTorrent-like peer-to-peer file sharing
protocol (handshake, bitfield, interested/not-interested, choke/unchoke,
have, request, piece — plus preferred-neighbor and optimistic-unchoke
selection).

---

## Group Members

_Fill in your group members here._

---

## Requirements

- Java JDK 8 or higher

---

## Startup

### 1. Configure

- `config/Common.cfg` — protocol parameters. Set `FileName`, `FileSize`,
  `PieceSize`, `NumberOfPreferredNeighbors`, `UnchokingInterval`,
  `OptimisticUnchokingInterval`.
- `config/PeerInfo.cfg` — one line per peer: `peerId host port hasFile`.
  Only list peers you are actually going to run; a peer waits for *every*
  other peer in `PeerInfo.cfg` to finish before terminating.
- For any peer marked `hasFile=1`, place the seed file at
  `peer_<peerId>/<FileName>` (or at `sample-data/peers/<peerId>/<FileName>`
  — the peer will copy it into `peer_<peerId>/` on startup).

### 2. Compile

From the repository root:

```bash
javac -d build/classes src/*.java
```

### 3. Launch Peers

Start peers **in the order they appear in `PeerInfo.cfg`**. Each peer
opens outgoing connections to every peer listed before it and accepts
incoming connections from every peer listed after it.

Run a single peer manually:

```bash
java -cp build/classes peerProcess <peerId>
```

Or use the launcher scripts to start a local group of peers:

**Linux / macOS / WSL:**

```bash
bash scripts/startup.sh              # launches 1001 1002 1003 by default
bash scripts/startup.sh 1001 1002    # or specify peer IDs
```

**Windows** (opens each peer in its own PowerShell window):

```bat
.\scripts\startup.bat
```

### 4. Output

- `peer_<id>/<FileName>` — final reassembled file on each peer
- `logs/log_peer_<id>.log` — per-peer structured log
- `logs/peer_<id>.out` — per-peer stdout/stderr (Linux/macOS launcher only)

Every peer exits with status 0 once it discovers that itself and every
other peer in `PeerInfo.cfg` has the complete file.

---

## File Structure

```
BitTorrent-Clone/
├── config/
│   ├── Common.cfg         # Protocol settings
│   └── PeerInfo.cfg       # Peer IDs, hosts, ports, seed flags
├── scripts/
│   ├── startup.sh         # Linux/macOS/WSL launcher
│   └── startup.bat        # Windows launcher
├── src/
│   ├── peerProcess.java   # Main entry point, schedulers, termination
│   ├── peerConnection.java# Per-neighbor protocol + message handlers
│   ├── peerState.java     # Per-neighbor bitfield, interest, choke state
│   ├── Message.java       # Wire message type constants
│   ├── Handshake.java     # 32-byte handshake encode/decode
│   ├── Logger.java        # Thread-safe timestamped logger
│   ├── Parser.java        # Standalone config parser utility
│   ├── FileManager.java   # Piece storage, disk I/O, file reassembly
│   └── NeighborManager.java# Preferred/optimistic neighbor selection
├── sample-data/peers/
│   ├── 1001/thefile       # Example seed (2.1 MB)
│   └── 1006/thefile
└── archive/
    └── proj1.tar
```

---

## Protocol Summary

- **Handshake:** 32 bytes = 18-byte header `P2PFILESHARINGPROJ` + 10 zero
  bytes + 4-byte peer ID.
- **Messages:** 4-byte length + 1-byte type + payload. Types: `CHOKE(0)`,
  `UNCHOKE(1)`, `INTERESTED(2)`, `NOT_INTERESTED(3)`, `HAVE(4)`,
  `BITFIELD(5)`, `REQUEST(6)`, `PIECE(7)`.
- **Piece selection:** random among pieces the neighbor has, we don't
  have, and haven't been requested from any neighbor yet. Only one
  outstanding request at a time per peer.
- **Preferred neighbors:** reselected every `UnchokingInterval` seconds.
  Top `k` interested peers by bytes received in the last interval;
  ties broken randomly. A seeder selects randomly from interested peers.
- **Optimistic unchoke:** reselected every `OptimisticUnchokingInterval`
  seconds. Random choice from interested peers that are currently choked.
- **Termination:** once every peer in `PeerInfo.cfg` holds the full
  bitfield, the process exits 0.

---

## Verification

After a successful run:

```bash
diff peer_1001/thefile peer_1002/thefile   # should show no differences
md5 peer_*/thefile                          # all hashes match
```

The 11 required log events can be grep'd across all peer logs:

```
makes a connection   |   is connected from   |   preferred neighbors
optimistically unchoked   |   is choked by   |   is unchoked by
received the 'have'   |   received the 'interested'
received the 'not interested'   |   has downloaded the piece
has downloaded the complete
```
