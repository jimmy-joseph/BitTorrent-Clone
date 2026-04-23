import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class peerProcess {

    private static Path resolveConfigPath(String name) {
        Path inConfig = Paths.get("config", name);
        if (Files.exists(inConfig)) return inConfig;
        Path inCwd = Paths.get(name);
        if (Files.exists(inCwd)) return inCwd;
        throw new RuntimeException(
            "Cannot find " + name + " in either config/ or current directory");
    }

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

    static NeighborManager neighborManager;
    static FileManager fileManager;
    static Set<Integer> requestedPieces = ConcurrentHashMap.newKeySet();
    static ScheduledExecutorService scheduler;

    static volatile boolean terminated = false;

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

        boolean hasFile = peers.get(peerId).hasFile;
        fileManager = new FileManager(peerId, fileName, fileSize, pieceSize, numPieces, hasFile);
        neighborManager = new NeighborManager();

        int port = peers.get(peerId).port;
        serverSocket = new ServerSocket(port);

        Logger.log("Peer " + peerId + " started on port " + port);

        connectToPreviousPeers();

        startSchedulers();

        Thread listenerThread = new Thread(peerProcess::listenLoop, "listener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        // Keep main alive until termination
        while (!terminated) {
            try { Thread.sleep(500); } catch (InterruptedException e) { break; }
        }
    }

    static void readCommonConfig() throws Exception {
        BufferedReader br = Files.newBufferedReader(resolveConfigPath("Common.cfg"));
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 2) continue;
            switch (parts[0]) {
                case "NumberOfPreferredNeighbors": numPreferredNeighbors = Integer.parseInt(parts[1]); break;
                case "UnchokingInterval":          unchokingInterval = Integer.parseInt(parts[1]); break;
                case "OptimisticUnchokingInterval":optimisticUnchokingInterval = Integer.parseInt(parts[1]); break;
                case "FileName":                   fileName = parts[1]; break;
                case "FileSize":                   fileSize = Integer.parseInt(parts[1]); break;
                case "PieceSize":                  pieceSize = Integer.parseInt(parts[1]); break;
            }
        }
        br.close();
    }

    static void readPeerInfo() throws Exception {
        BufferedReader br = Files.newBufferedReader(resolveConfigPath("PeerInfo.cfg"));
        String line;
        while ((line = br.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String[] parts = trimmed.split("\\s+");
            if (parts.length < 4) continue;
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
            for (int i = 0; i < bitfieldLen; i++) bitfield[i] = (byte) 0xFF;
            int spareBits = bitfieldLen * 8 - numPieces;
            if (spareBits > 0) bitfield[bitfieldLen - 1] &= (byte) (0xFF << spareBits);
        }
    }

    static void connectToPreviousPeers() throws Exception {
        for (PeerInfo p : peers.values()) {
            if (p.id == peerId) break;

            try {
                Socket socket = new Socket(p.host, p.port);
                Logger.log("Peer " + peerId + " makes a connection to Peer " + p.id);
                new peerConnection(socket, p.id).start();
            } catch (Exception e) {
                System.out.println("Failed to connect to peer " + p.id + ": " + e.getMessage());
            }
        }
    }

    static void listenLoop() {
        while (!terminated) {
            try {
                Socket socket = serverSocket.accept();
                peerConnection connection = new peerConnection(socket);
                connection.start();
            } catch (IOException e) {
                if (!terminated) e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void startSchedulers() {
        scheduler = Executors.newScheduledThreadPool(3);

        scheduler.scheduleAtFixedRate(() -> {
            try { neighborManager.selectPreferredNeighbors(); }
            catch (Throwable t) { t.printStackTrace(); }
        }, unchokingInterval, unchokingInterval, TimeUnit.SECONDS);

        scheduler.scheduleAtFixedRate(() -> {
            try { neighborManager.selectOptimisticUnchoke(); }
            catch (Throwable t) { t.printStackTrace(); }
        }, optimisticUnchokingInterval, optimisticUnchokingInterval, TimeUnit.SECONDS);

        scheduler.scheduleAtFixedRate(peerProcess::checkTermination,
                2, 2, TimeUnit.SECONDS);
    }

    public static synchronized void onPieceReceived(int pieceIdx, byte[] data, int fromPeerId, peerConnection conn) {
        boolean accepted = fileManager.setPiece(pieceIdx, data);
        requestedPieces.remove(pieceIdx);

        if (!accepted) {
            // duplicate; still try to request next
            try {
                if (!fileManager.isComplete()) conn.requestNextPiece();
            } catch (IOException ignored) {}
            return;
        }

        int byteIdx = pieceIdx / 8;
        int bitIdx = 7 - (pieceIdx % 8);
        bitfield[byteIdx] |= (byte) (1 << bitIdx);

        peerState fromPeer = neighborManager.neighbors.get(fromPeerId);
        if (fromPeer != null) {
            fromPeer.bytesDownloadedThisInterval += data.length;
        }

        int numHave = fileManager.numPiecesHeld();

        Logger.log("Peer " + peerId + " has downloaded the piece " + pieceIdx
                + " from " + fromPeerId + ". Now the number of pieces it has is " + numHave);

        neighborManager.broadcastHave(pieceIdx);
        neighborManager.reevaluateAllInterest();

        if (fileManager.isComplete()) {
            Logger.log("Peer " + peerId + " has downloaded the complete file.");
            try { fileManager.writeFullFileIfNeeded(); }
            catch (IOException e) { e.printStackTrace(); }
        }

        if (fromPeer != null && !fromPeer.chokingMe && !fileManager.isComplete()) {
            try { conn.requestNextPiece(); } catch (IOException ignored) {}
        }

        checkTermination();
    }

    public static BitSet getMyBitSet() {
        BitSet bs = new BitSet(numPieces);
        for (int i = 0; i < numPieces; i++) {
            int byteIdx = i / 8;
            int bitIdx = 7 - (i % 8);
            if ((bitfield[byteIdx] & (1 << bitIdx)) != 0) bs.set(i);
        }
        return bs;
    }

    public static void checkTermination() {
        if (terminated) return;
        if (!fileManager.isComplete()) return;
        for (PeerInfo p : peers.values()) {
            if (p.id == peerId) continue;
            peerState st = neighborManager.neighbors.get(p.id);
            if (st == null) return;
            if (st.pieces.cardinality() < numPieces) return;
        }
        terminated = true;
        Logger.log("All peers have the complete file. Shutting down.");
        try { if (scheduler != null) scheduler.shutdownNow(); } catch (Exception ignored) {}
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
        // give log flush a moment
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        System.exit(0);
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
