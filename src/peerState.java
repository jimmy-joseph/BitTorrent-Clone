import java.util.BitSet;

public class peerState {

    public int peerId;

    public BitSet pieces;

    // whether this peer is interested in our pieces
    public boolean interested = false;

    // whether we have declared interest in this peer's pieces
    public boolean iAmInterested = false;

    // whether we are currently choking this peer
    public boolean choked = true;

    // whether this peer is choking us
    public boolean chokingMe = true;

    // bytes received from this peer during the current unchoking interval
    public int bytesDownloadedThisInterval = 0;

    public peerState(int peerId, int numPieces) {
        this.peerId = peerId;
        this.pieces = new BitSet(numPieces);
    }
}
