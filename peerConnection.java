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

        System.out.println("test 1");

        in = new DataInputStream(socket.getInputStream());
        System.out.println("test 2");

        out = new DataOutputStream(socket.getOutputStream());
<<<<<<< HEAD

        System.out.println("test 3");

        sendHandshake();

        System.out.println("test 4");

        receiveHandshake();

        System.out.println("test 5");

=======
>>>>>>> 1851c2642446c182ca396654782d88f9e63f59f9
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

        System.out.println("test 6");

        byte[] handshake = new byte[32];

        in.readFully(handshake);

<<<<<<< HEAD
                System.out.println("test 7");


        int peerId = Handshake.extractPeerId(handshake);
=======
        remotePeerId = Handshake.extractPeerId(handshake);
>>>>>>> 1851c2642446c182ca396654782d88f9e63f59f9

        System.out.println("Received handshake from peer " + remotePeerId);
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