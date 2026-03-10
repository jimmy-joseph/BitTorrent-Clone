import java.io.*;
import java.net.*;

public class peerConnection extends Thread {

    Socket socket;
    int remotePeerId;
    boolean isIncoming;

    DataInputStream in;
    DataOutputStream out;

    public peerConnection(Socket socket, int remotePeerId, boolean isIncoming) throws Exception {

        this.socket = socket;
        this.remotePeerId = remotePeerId;
        this.isIncoming = isIncoming;

        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        sendHandshake();
        if (!isIncoming)
            receiveHandshake();
        else
            Logger.log("Peer " + peerProcess.peerId + " received handshake from Peer " + remotePeerId);

        if (isIncoming) {
            receiveBitfield();
            sendBitfield();
        } else {
            sendBitfield();
            receiveBitfield();
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

        if (!Handshake.header.equals(new String(handshake, 0, 18, java.nio.charset.StandardCharsets.US_ASCII)))
            throw new IOException("Invalid handshake header");

        int receivedPeerId = Handshake.extractPeerId(handshake);
        if (receivedPeerId != remotePeerId)
            throw new IOException("Unexpected peer ID: expected " + remotePeerId + ", got " + receivedPeerId);

        Logger.log("Peer " + peerProcess.peerId + " received handshake from Peer " + receivedPeerId);
    }

    void sendBitfield() throws Exception {

        if (!hasAnyPieces())
            return;

        out.writeInt(1 + peerProcess.bitfield.length);
        out.writeByte(5);
        out.write(peerProcess.bitfield);
        out.flush();

        Logger.log("Peer " + peerProcess.peerId + " sent bitfield to Peer " + remotePeerId);
    }

    void receiveBitfield() throws Exception {

        int length = in.readInt();
        byte type = in.readByte();
        byte[] payload = new byte[length - 1];
        if (payload.length > 0)
            in.readFully(payload);

        if (type == 5) {
            remoteBitfield = payload;
            Logger.log("Peer " + peerProcess.peerId + " received bitfield from Peer " + remotePeerId);
        } else {
            remoteBitfield = new byte[(peerProcess.numPieces + 7) / 8];
            if (type == 2 || type == 3)
                handleMessage(type, payload);
        }
    }

    byte[] remoteBitfield;

    boolean hasAnyPieces() {

        for (byte b : peerProcess.bitfield)
            if (b != 0) return true;
        return false;
    }

    boolean peerHasPiecesWeNeed(byte[] peerBitfield) {

        for (int i = 0; i < peerProcess.numPieces; i++) {
            int byteIdx = i / 8;
            if (byteIdx >= peerBitfield.length)
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

        Logger.log("Peer " + peerProcess.peerId + " sent " + (interested ? "INTERESTED" : "NOT_INTERESTED") + " to Peer " + remotePeerId);
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