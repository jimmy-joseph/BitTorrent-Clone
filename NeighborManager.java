import java.util.*;

/*

public class NeighborManager {

    Map<Integer, peerState> neighbors = new HashMap<>();

    Set<Integer> preferredNeighbors = new HashSet<>();

    Integer optimisticNeighbor = null;

    //needed for messaging
    Map<Integer, peerConnection> connections = new HashMap<>();


    //UPDATECHOKING
    public void updateChoking() {

        for (peerState peer : neighbors.values()) {

            boolean shouldBeUnchoked =
                preferredNeighbors.contains(peer.peerId) ||
                (optimisticNeighbor != null && peer.peerId == optimisticNeighbor);

            if (shouldBeUnchoked) {

                if (peer.choked) {
                    sendUnchoke(peer.peerId);
                    peer.choked = false;
                }

            } else {

                if (!peer.choked) {
                    sendChoke(peer.peerId);
                    peer.choked = true;
                }

            }
        }
    }

    //UPDATEINTEREST

    public void updateInterest(peerState peer, BitSet myPieces) {

        // Copy neighbor pieces
        BitSet temp = (BitSet) peer.pieces.clone();

        // Remove pieces we already have
        temp.andNot(myPieces);

        boolean interestedNow = !temp.isEmpty();

        if (interestedNow && !peer.interested) {
            sendInterested(peer.peerId);
            peer.interested = true;
        }

        if (!interestedNow && peer.interested) {
            sendNotInterested(peer.peerId);
            peer.interested = false;
        }
    }
/*
    private void sendChoke(int peerId) {
        try {
            connections.get(peerId).sendChoke();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendUnchoke(int peerId) {
        try {
            connections.get(peerId).sendUnchoke();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void sendNotInterested(int peerId) {
        try {
            connections.get(peerId).sendNotInterested();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void sendInterested(int peerId) {
        try {
            connections.get(peerId).sendInterested();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
        
}

*/
