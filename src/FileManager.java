import java.io.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FileManager {

    final int peerId;
    final String fileName;
    final int fileSize;
    final int pieceSize;
    final int numPieces;
    final byte[][] pieceList;
    final Path peerDir;
    final AtomicInteger numHeld = new AtomicInteger(0);
    volatile boolean fullFileWritten = false;

    public FileManager(int peerId, String fileName, int fileSize, int pieceSize, int numPieces, boolean hasFile) throws IOException {
        this.peerId = peerId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;
        this.numPieces = numPieces;
        this.pieceList = new byte[numPieces][];
        this.peerDir = Paths.get("peer_" + peerId);
        Files.createDirectories(peerDir);

        if (hasFile) {
            loadFile();
        }
    }

    private void loadFile() throws IOException {
        Path filePath = peerDir.resolve(fileName);
        if (!Files.exists(filePath)) {
            Path seedPath = Paths.get("sample-data", "peers", String.valueOf(peerId), fileName);
            if (Files.exists(seedPath)) {
                Files.copy(seedPath, filePath);
            } else {
                throw new FileNotFoundException("Seed file not found at " + filePath + " or " + seedPath);
            }
        }

        byte[] all = Files.readAllBytes(filePath);
        for (int i = 0; i < numPieces; i++) {
            int start = i * pieceSize;
            int end = Math.min(start + pieceSize, fileSize);
            byte[] piece = new byte[end - start];
            System.arraycopy(all, start, piece, 0, piece.length);
            pieceList[i] = piece;
        }
        numHeld.set(numPieces);
        fullFileWritten = true;
    }

    public byte[] getPiece(int index) {
        return pieceList[index];
    }

    public synchronized boolean setPiece(int index, byte[] data) {
        if (pieceList[index] != null) return false;
        pieceList[index] = data;
        numHeld.incrementAndGet();
        return true;
    }

    public boolean hasPiece(int index) {
        return pieceList[index] != null;
    }

    public int numPiecesHeld() {
        return numHeld.get();
    }

    public boolean isComplete() {
        return numHeld.get() == numPieces;
    }

    public synchronized void writeFullFileIfNeeded() throws IOException {
        if (fullFileWritten || !isComplete()) return;
        Path out = peerDir.resolve(fileName);
        try (FileOutputStream fos = new FileOutputStream(out.toFile())) {
            for (int i = 0; i < numPieces; i++) {
                fos.write(pieceList[i]);
            }
        }
        fullFileWritten = true;
    }
}
