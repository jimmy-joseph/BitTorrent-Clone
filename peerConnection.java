import java.io.*;
import java.net.*;

public class peerConnection extends Thread {

    Socket socket;
    int remotePeerId;
    boolean isOutgoing;
    byte[] neighborBitfield;

    DataInputStream in;
    DataOutputStream out;

    // outgoing connection — we initiated it, remotePeerId is known
    public peerConnection(Socket socket, int remotePeerId) throws Exception {

        this.socket = socket;
        this.remotePeerId = remotePeerId;
        this.isOutgoing = true;

        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    // incoming connection — remotePeerId learned from handshake
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

            sendBitfieldIfNeeded();

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

        remotePeerId = Handshake.extractPeerId(handshake);

        System.out.println("Received handshake from peer " + remotePeerId);

        // added
        if (!isOutgoing)
            Logger.log("Peer " + peerProcess.peerId + " is connected from Peer " + remotePeerId);
    }

    void sendMessage(Message msg) throws Exception {
        int length = 1 + msg.payload.length;
        out.writeInt(length);
        out.writeByte(msg.type);
        if (msg.payload.length > 0){
            out.write(msg.payload);
        }
        
        out.flush();
    }

    void sendBitfieldIfNeeded() throws IOException {
        // If this peer has no pieces (all zeros), you may skip sending bitfield
        boolean hasAnyPiece = false;
        for (byte b : peerProcess.bitfield) {
            if (b != 0) {
                hasAnyPiece = true;
                break;
            }
        }
        if (!hasAnyPiece) {
            return; // skip bitfield if we have nothing
        }
        Message bitfieldMsg = new Message(Message.BITFIELD, peerProcess.bitfield);
        sendMessage(bitfieldMsg);
    }

    void handleBitfield(byte[] bitfield) throws Exception {
        neighborBitfield = bitfield;

        boolean interested = false;
        for (int i = 0; i < peerProcess.bitfield.length && i < neighborBitfield.length; i++) {
            if ((neighborBitfield[i] & ~peerProcess.bitfield[i]) != 0) {
                interested = true;
                break;
            }
        }

        if (interested) {
            sendMessage(new Message(Message.INTERESTED));
        } else {
            sendMessage(new Message(Message.NOT_INTERESTED));
        }
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
            /*
            case 5:
                try {
                    handleBitfield(payload);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            */
        }
    }
}