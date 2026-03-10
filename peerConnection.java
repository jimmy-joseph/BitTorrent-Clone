import java.io.*;
import java.net.*;

public class peerConnection extends Thread {

    Socket socket;
    int remotePeerId;

    DataInputStream in;
    DataOutputStream out;

    public peerConnection(Socket socket, int remotePeerId) throws Exception {

        this.socket = socket;
        this.remotePeerId = remotePeerId;

        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    public void run() {

        try {

            sendHandshake();
            receiveHandshake();

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
        }
    }
}