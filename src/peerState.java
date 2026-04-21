import java.util.BitSet;

public class peerState {

    // ID of the peer
    public int peerId;

    // pieces this peer currently has
    public BitSet pieces;

    // whether this peer is interested in our pieces
    public boolean interested = false;

    // whether we are currently choking this peer
    public boolean choked = true;

    // whether this peer is choking us
    public boolean chokingMe = true;

    // number of pieces received from this peer during the current interval
    public int downloadRate = 0;

    public peerState(int peerId, int numPieces) {
        this.peerId = peerId;
        this.pieces = new BitSet(numPieces);
    }
}