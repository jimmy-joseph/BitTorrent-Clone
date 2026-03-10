import java.io.*;
import java.net.*;
import java.util.*;

public class peerProcess {

    static int peerId;
    static Map<Integer, PeerInfo> peers = new LinkedHashMap<>();

    static int numPreferredNeighbors;
    static int unchokingInterval;
    static int optimisticUnchokingInterval;
    static String fileName;
    static int fileSize;
    static int pieceSize;

    static int numPieces;
    static byte[] bitfield;

    static ServerSocket serverSocket;

    public static void main(String[] args) throws Exception {

        if (args.length != 1) {
            System.out.println("Usage: java peerProcess <peerId>");
            return;
        }

        peerId = Integer.parseInt(args[0]);

        readCommonConfig();
        readPeerInfo();

        initBitfield();

        Logger.init(peerId);

        int port = peers.get(peerId).port;

        serverSocket = new ServerSocket(port);

        Logger.log("Peer " + peerId + " started on port " + port);

        connectToPreviousPeers();

        listenForConnections();
    }

    static void readCommonConfig() throws Exception {

        BufferedReader br = new BufferedReader(new FileReader("Common.cfg"));

        String line;

        while ((line = br.readLine()) != null) {

            String[] parts = line.split(" ");

            switch (parts[0]) {

                case "NumberOfPreferredNeighbors":
                    numPreferredNeighbors = Integer.parseInt(parts[1]);
                    break;

                case "UnchokingInterval":
                    unchokingInterval = Integer.parseInt(parts[1]);
                    break;

                case "OptimisticUnchokingInterval":
                    optimisticUnchokingInterval = Integer.parseInt(parts[1]);
                    break;

                case "FileName":
                    fileName = parts[1];
                    break;

                case "FileSize":
                    fileSize = Integer.parseInt(parts[1]);
                    break;

                case "PieceSize":
                    pieceSize = Integer.parseInt(parts[1]);
                    break;
            }
        }

        br.close();
    }

    static void readPeerInfo() throws Exception {

        BufferedReader br = new BufferedReader(new FileReader("PeerInfo.cfg"));

        String line;

        while ((line = br.readLine()) != null) {

            String[] parts = line.split(" ");

            int id = Integer.parseInt(parts[0]);
            String host = parts[1];
            int port = Integer.parseInt(parts[2]);
            int hasFile = Integer.parseInt(parts[3]);

            peers.put(id, new PeerInfo(id, host, port, hasFile == 1));
        }

        br.close();
    }

    static void initBitfield() {

        numPieces = (int) Math.ceil((double) fileSize / pieceSize);
        int bitfieldLen = (numPieces + 7) / 8;
        bitfield = new byte[bitfieldLen];

        boolean hasFile = peers.get(peerId).hasFile;
        if (hasFile) {
            // set all bits to 1
            for (int i = 0; i < bitfieldLen; i++)
                bitfield[i] = (byte) 0xFF; // set bits to 1
            int spareBits = bitfieldLen * 8 - numPieces;
            if (spareBits > 0)
                bitfield[bitfieldLen - 1] &= (byte) (0xFF << spareBits);
        }
        // if file not found, bit set to 0 by default
    }

    static void connectToPreviousPeers() throws Exception {

        for (PeerInfo p : peers.values()) {

            if (p.id == peerId)
                break;

            Socket socket = new Socket(p.host, p.port);

            Logger.log("Peer " + peerId + " makes a connection to Peer " + p.id);

            new peerConnection(socket, p.id).start();
        }
    }

    static void listenForConnections() throws Exception {

        while (true) {

            Socket socket = serverSocket.accept();

            DataInputStream in = new DataInputStream(socket.getInputStream());

            byte[] handshake = new byte[32];

            in.readFully(handshake);

            int remotePeerId = Handshake.extractPeerId(handshake);

            Logger.log("Peer " + peerId + " is connected from Peer " + remotePeerId);

            peerConnection connection = new peerConnection(socket, remotePeerId);

            connection.start();
        }
    }
}

class PeerInfo {

    int id;
    String host;
    int port;
    boolean hasFile;

    PeerInfo(int id, String host, int port, boolean hasFile) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.hasFile = hasFile;
    }
}