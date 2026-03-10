/* public void updateChoking() {

    for (PeerState peer : neighbors.values()) {

        boolean shouldBeUnchoked =
            preferredNeighbors.contains(peer.peerId) ||
            (optimisticNeighbor != null && peer.peerId == optimisticNeighbor);

        if (shouldBeUnchoked) {

            // If currently choked but should not be → unchoke
            if (peer.choked) {
                sendUnchoke(peer.peerId);
                peer.choked = false;
            }

        } else {

            // If currently unchoked but should be → choke
            if (!peer.choked) {
                sendChoke(peer.peerId);
                peer.choked = true;
            }

        }
    }
}
*/

