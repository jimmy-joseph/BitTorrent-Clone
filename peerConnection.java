import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class peerConnection extends Thread {

    Socket socket;
    int remotePeerId;
    boolean isOutgoing;

    DataInputStream in;
    DataOutputStream out;

    volatile boolean running = true;

    // outgoing connection
    public peerConnection(Socket socket, int remotePeerId) throws Exception {
        this.socket = socket;
        this.remotePeerId = remotePeerId;
        this.isOutgoing = true;

        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    // incoming connection
    public peerConnection(Socket socket) throws Exception {
        this.socket = socket;
        this.remotePeerId = -1;
        this.isOutgoing = false;

        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    public void run() {
        try {
            if (isOutgoing) {
                sendHandshake();
                receiveHandshake();
            } else {
                receiveHandshake();
                sendHandshake();
            }

            peerProcess.neighborManager.registerPeer(remotePeerId, peerProcess.numPieces);
            peerProcess.neighborManager.registerConnection(remotePeerId, this);

            sendBitfieldIfNeeded();

            while (running) {
                int length = in.readInt();
                byte type = in.readByte();

                byte[] payload = new byte[length - 1];
                if (payload.length > 0)
                    in.readFully(payload);

                handleMessage(type, payload);
            }

        } catch (EOFException e) {
        } catch (IOException e) {
            if (running) {
                System.out.println("Connection to peer " + remotePeerId + " lost: " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendHandshake() throws IOException {
        byte[] handshake = Handshake.create(peerProcess.peerId);
        out.write(handshake);
        out.flush();
    }

    void receiveHandshake() throws IOException {
        byte[] handshake = new byte[32];
        in.readFully(handshake);
        remotePeerId = Handshake.extractPeerId(handshake);

        if (!isOutgoing) {
            Logger.log("Peer " + peerProcess.peerId + " is connected from Peer " + remotePeerId);
        }
    }

    synchronized void sendMessage(Message msg) throws IOException {
        int length = 1 + msg.payload.length;
        out.writeInt(length);
        out.writeByte(msg.type);
        if (msg.payload.length > 0) {
            out.write(msg.payload);
        }
        out.flush();
    }

    void sendBitfieldIfNeeded() throws IOException {
        boolean hasAnyPiece = false;
        for (byte b : peerProcess.bitfield) {
            if (b != 0) {
                hasAnyPiece = true;
                break;
            }
        }
        if (!hasAnyPiece) return;

        sendMessage(new Message(Message.BITFIELD, peerProcess.bitfield.clone()));
    }

    public void sendChoke() throws IOException {
        sendMessage(new Message(Message.CHOKE));
    }

    public void sendUnchoke() throws IOException {
        sendMessage(new Message(Message.UNCHOKE));
    }

    public void sendInterested() throws IOException {
        sendMessage(new Message(Message.INTERESTED));
    }

    public void sendNotInterested() throws IOException {
        sendMessage(new Message(Message.NOT_INTERESTED));
    }

    public void sendHave(int pieceIndex) throws IOException {
        byte[] payload = ByteBuffer.allocate(4).putInt(pieceIndex).array();
        sendMessage(new Message(Message.HAVE, payload));
    }

    public void sendRequest(int pieceIndex) throws IOException {
        byte[] payload = ByteBuffer.allocate(4).putInt(pieceIndex).array();
        sendMessage(new Message(Message.REQUEST, payload));
    }

    public void sendPieceMsg(int pieceIndex) throws IOException {
        byte[] piece = peerProcess.fileManager.getPiece(pieceIndex);
        if (piece == null) return;
        ByteBuffer buf = ByteBuffer.allocate(4 + piece.length);
        buf.putInt(pieceIndex);
        buf.put(piece);
        sendMessage(new Message(Message.PIECE, buf.array()));
    }

    void handleMessage(byte type, byte[] payload) throws IOException {
        peerState peer = peerProcess.neighborManager.neighbors.get(remotePeerId);

        switch (type) {

            case Message.CHOKE: {
                peer.chokingMe = true;
                Logger.log("Peer " + peerProcess.peerId + " is choked by " + remotePeerId);
                break;
            }

            case Message.UNCHOKE: {
                peer.chokingMe = false;
                Logger.log("Peer " + peerProcess.peerId + " is unchoked by " + remotePeerId);
                requestNextPiece();
                break;
            }

            case Message.INTERESTED: {
                peer.interested = true;
                Logger.log("Peer " + peerProcess.peerId + " received the 'interested' message from " + remotePeerId);
                break;
            }

            case Message.NOT_INTERESTED: {
                peer.interested = false;
                Logger.log("Peer " + peerProcess.peerId + " received the 'not interested' message from " + remotePeerId);
                break;
            }

            case Message.HAVE: {
                int pieceIdx = ByteBuffer.wrap(payload).getInt();
                peer.pieces.set(pieceIdx);
                Logger.log("Peer " + peerProcess.peerId + " received the 'have' message from " + remotePeerId + " for the piece " + pieceIdx);
                updateInterestIn(peer);
                peerProcess.checkTermination();
                break;
            }

            case Message.BITFIELD: {
                handleBitfield(payload);
                break;
            }

            case Message.REQUEST: {
                int reqIdx = ByteBuffer.wrap(payload).getInt();
                handleRequest(reqIdx);
                break;
            }

            case Message.PIECE: {
                handlePiece(payload);
                break;
            }
        }
    }

    void handleBitfield(byte[] bitfieldBytes) throws IOException {
        peerState peer = peerProcess.neighborManager.neighbors.get(remotePeerId);
        for (int i = 0; i < peerProcess.numPieces; i++) {
            int byteIdx = i / 8;
            int bitIdx = 7 - (i % 8);
            if (byteIdx < bitfieldBytes.length && (bitfieldBytes[byteIdx] & (1 << bitIdx)) != 0) {
                peer.pieces.set(i);
            }
        }
        updateInterestIn(peer);
    }

    public void updateInterestIn(peerState peer) throws IOException {
        BitSet myPieces = peerProcess.getMyBitSet();
        BitSet theirPieces = (BitSet) peer.pieces.clone();
        theirPieces.andNot(myPieces);
        boolean interesting = !theirPieces.isEmpty();

        if (interesting && !peer.iAmInterested) {
            sendInterested();
            peer.iAmInterested = true;
        } else if (!interesting && peer.iAmInterested) {
            sendNotInterested();
            peer.iAmInterested = false;
        }
    }

    void handleRequest(int pieceIndex) throws IOException {
        peerState peer = peerProcess.neighborManager.neighbors.get(remotePeerId);
        if (peer.choked) {
            return; // ignore
        }
        if (!peerProcess.fileManager.hasPiece(pieceIndex)) return;
        sendPieceMsg(pieceIndex);
    }

    void handlePiece(byte[] payload) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(payload);
        int pieceIdx = buf.getInt();
        byte[] data = new byte[payload.length - 4];
        buf.get(data);
        peerProcess.onPieceReceived(pieceIdx, data, remotePeerId, this);
    }

    public void requestNextPiece() throws IOException {
        peerState peer = peerProcess.neighborManager.neighbors.get(remotePeerId);
        if (peer == null || peer.chokingMe) return;

        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < peerProcess.numPieces; i++) {
            if (peer.pieces.get(i)
                    && !peerProcess.fileManager.hasPiece(i)
                    && !peerProcess.requestedPieces.contains(i)) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) return;

        Collections.shuffle(candidates);
        for (int candidate : candidates) {
            if (peerProcess.requestedPieces.add(candidate)) {
                sendRequest(candidate);
                return;
            }
        }
    }
}
