import java.io.*;
import java.net.*;

public class peerConnection extends Thread {

    Socket socket;
    int remotePeerId;
    boolean isIncoming;

    DataInputStream in;
    DataOutputStream out;

    byte[] remoteBitfield;

    public peerConnection(Socket socket, int remotePeerId, boolean isIncoming) throws Exception {

        this.socket = socket;
        this.remotePeerId = remotePeerId;
        this.isIncoming = isIncoming;

        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        sendHandshake();

        // For incoming connections, `peerProcess` already read and validated the remote handshake.
        if (!isIncoming)
            receiveHandshake();
        else
            Logger.log("Peer " + peerProcess.peerId + " received handshake from Peer " + remotePeerId);

        // Immediately after handshake, exchange bitfields (if a peer has no pieces, it may skip).
        if (isIncoming) {
            receiveBitfieldOrSkip();
            sendBitfieldIfAny();
        } else {
            sendBitfieldIfAny();
            receiveBitfieldOrSkip();
        }

        sendInterestedOrNotInterested();
    }

    public void run() {

        try {

            while (true) {

                int length = in.readInt();
                byte type = in.readByte();

                byte[] payload = new byte[length - 1];

                if (payload.length > 0)
                    in.readFully(payload);

                handleMessage(type, payload);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendHandshake() throws Exception {

        byte[] handshake = Handshake.create(peerProcess.peerId);

        out.write(handshake);
        out.flush();
    }

    void receiveHandshake() throws Exception {

        byte[] handshake = new byte[32];

        in.readFully(handshake);

        String receivedHeader = new String(handshake, 0, 18, java.nio.charset.StandardCharsets.US_ASCII);
        if (!Handshake.header.equals(receivedHeader))
            throw new IOException("Invalid handshake header");

        int receivedPeerId = Handshake.extractPeerId(handshake);
        if (receivedPeerId != remotePeerId)
            throw new IOException("Unexpected peer ID: expected " + remotePeerId + ", got " + receivedPeerId);

        Logger.log("Peer " + peerProcess.peerId + " received handshake from Peer " + receivedPeerId);
    }

    void sendBitfieldIfAny() throws Exception {

        if (!hasAnyPieces())
            return;

        out.writeInt(1 + peerProcess.bitfield.length);
        out.writeByte(5);
        out.write(peerProcess.bitfield);
        out.flush();
    }

    void receiveBitfieldOrSkip() throws Exception {

        int length = in.readInt();
        byte type = in.readByte();

        byte[] payload = new byte[length - 1];
        if (payload.length > 0)
            in.readFully(payload);

        if (type == 5) {
            remoteBitfield = payload;
        } else {
            // Peer may skip bitfield if it has no pieces.
            remoteBitfield = new byte[(peerProcess.numPieces + 7) / 8];
            // If we already consumed an interested/not interested message, handle it now.
            if (type == 2 || type == 3)
                handleMessage(type, payload);
        }
    }

    boolean hasAnyPieces() {

        if (peerProcess.bitfield == null)
            return false;

        for (byte b : peerProcess.bitfield)
            if (b != 0)
                return true;
        return false;
    }

    boolean peerHasPiecesWeNeed(byte[] peerBitfield) {

        if (peerBitfield == null)
            return false;

        for (int i = 0; i < peerProcess.numPieces; i++) {
            int byteIdx = i / 8;
            if (byteIdx >= peerBitfield.length || byteIdx >= peerProcess.bitfield.length)
                break;
            int bitIdx = 7 - (i % 8);
            boolean peerHas = (peerBitfield[byteIdx] & (1 << bitIdx)) != 0;
            boolean weHave = (peerProcess.bitfield[byteIdx] & (1 << bitIdx)) != 0;
            if (peerHas && !weHave)
                return true;
        }
        return false;
    }

    void sendInterestedOrNotInterested() throws Exception {

        boolean interested = peerHasPiecesWeNeed(remoteBitfield);

        out.writeInt(1);
        out.writeByte(interested ? 2 : 3);
        out.flush();
    }

    void handleMessage(byte type, byte[] payload) {

        switch (type) {

            case 0:
                Logger.log("Received CHOKE from " + remotePeerId);
                break;

            case 1:
                Logger.log("Received UNCHOKE from " + remotePeerId);
                break;

            case 2:
                Logger.log("Received INTERESTED from " + remotePeerId);
                break;

            case 3:
                Logger.log("Received NOT_INTERESTED from " + remotePeerId);
                break;

            case 5:
                Logger.log("Received BITFIELD from " + remotePeerId);
                break;
        }
    }
}