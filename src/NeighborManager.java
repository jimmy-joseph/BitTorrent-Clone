import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NeighborManager {

    public Map<Integer, peerState> neighbors = new ConcurrentHashMap<>();
    public Map<Integer, peerConnection> connections = new ConcurrentHashMap<>();
    public Set<Integer> preferredNeighbors = ConcurrentHashMap.newKeySet();
    public volatile Integer optimisticNeighbor = null;

    final Random random = new Random();

    public synchronized void registerPeer(int peerId, int numPieces) {
        neighbors.computeIfAbsent(peerId, id -> new peerState(id, numPieces));
    }

    public void registerConnection(int peerId, peerConnection conn) {
        connections.put(peerId, conn);
    }

    public void broadcastHave(int pieceIdx) {
        for (Map.Entry<Integer, peerConnection> e : connections.entrySet()) {
            try {
                e.getValue().sendHave(pieceIdx);
            } catch (IOException ex) {
            }
        }
    }

    public void reevaluateAllInterest() {
        for (peerConnection c : connections.values()) {
            peerState p = neighbors.get(c.remotePeerId);
            if (p == null) continue;
            try {
                c.updateInterestIn(p);
            } catch (IOException ex) {
            }
        }
    }

    public synchronized void selectPreferredNeighbors() {
        List<peerState> interested = new ArrayList<>();
        for (peerState p : neighbors.values()) {
            if (p.interested) interested.add(p);
        }

        Set<Integer> newPreferred = new HashSet<>();

        if (!interested.isEmpty()) {
            if (peerProcess.fileManager.isComplete()) {
                Collections.shuffle(interested, random);
            } else {
                Collections.shuffle(interested, random);
                interested.sort((a, b) -> Integer.compare(b.bytesDownloadedThisInterval, a.bytesDownloadedThisInterval));
            }
            int k = Math.min(peerProcess.numPreferredNeighbors, interested.size());
            for (int i = 0; i < k; i++) {
                newPreferred.add(interested.get(i).peerId);
            }
        }

        preferredNeighbors.clear();
        preferredNeighbors.addAll(newPreferred);

        logPreferred();
        applyChokingState();

        for (peerState p : neighbors.values()) {
            p.bytesDownloadedThisInterval = 0;
        }
    }

    private void logPreferred() {
        if (preferredNeighbors.isEmpty()) {
            Logger.log("Peer " + peerProcess.peerId + " has the preferred neighbors (none interested)");
            return;
        }
        List<Integer> sorted = new ArrayList<>(preferredNeighbors);
        Collections.sort(sorted);
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) list.append(",");
            list.append(sorted.get(i));
        }
        Logger.log("Peer " + peerProcess.peerId + " has the preferred neighbors " + list.toString());
    }

    public synchronized void selectOptimisticUnchoke() {
        List<peerState> candidates = new ArrayList<>();
        for (peerState p : neighbors.values()) {
            if (p.interested && p.choked && !preferredNeighbors.contains(p.peerId)) {
                candidates.add(p);
            }
        }
        if (candidates.isEmpty()) {
            // leave optimisticNeighbor as-is if still valid; otherwise clear
            if (optimisticNeighbor != null) {
                peerState cur = neighbors.get(optimisticNeighbor);
                if (cur == null || !cur.interested) {
                    optimisticNeighbor = null;
                    applyChokingState();
                }
            }
            return;
        }
        peerState chosen = candidates.get(random.nextInt(candidates.size()));
        optimisticNeighbor = chosen.peerId;
        Logger.log("Peer " + peerProcess.peerId + " has the optimistically unchoked neighbor " + optimisticNeighbor);
        applyChokingState();
    }

    synchronized void applyChokingState() {
        for (peerState peer : neighbors.values()) {
            boolean shouldUnchoke = preferredNeighbors.contains(peer.peerId)
                    || (optimisticNeighbor != null && peer.peerId == optimisticNeighbor.intValue());
            peerConnection conn = connections.get(peer.peerId);
            if (conn == null) continue;

            if (shouldUnchoke && peer.choked) {
                try { conn.sendUnchoke(); } catch (IOException e) {}
                peer.choked = false;
            } else if (!shouldUnchoke && !peer.choked) {
                try { conn.sendChoke(); } catch (IOException e) {}
                peer.choked = true;
            }
        }
    }
}
