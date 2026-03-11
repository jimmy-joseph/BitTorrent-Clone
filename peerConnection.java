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

            receiveHandshake();
            sendHandshake();

            listenForMessages();

        } catch (Exception e) {

            System.out.println("Connection closed with peer " + remotePeerId);
        }
    }

    void listenForMessages() throws Exception {

        while (true) {
            Message msg = Message.receive(in);
            handleMessage(msg);
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

    void sendMessage(byte type, byte[] payload) throws Exception {

        int length = 1 + (payload == null ? 0 : payload.length);

        Message msg = new Message((byte)2, null);
        msg.send(out);

        if (payload != null)
            out.write(payload);

        out.flush();
    }

    void handleMessage(Message msg) {

    switch(msg.type) {

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

        case 4:
            Logger.log("Received HAVE from " + remotePeerId);
            break;

        case 5:
            Logger.log("Received BITFIELD from " + remotePeerId);
            break;

        case 6:
            Logger.log("Received REQUEST from " + remotePeerId);
            break;

        case 7:
            Logger.log("Received PIECE from " + remotePeerId);
            break;
    }
}
}