import java.nio.ByteBuffer;

public class Handshake {

    static String header = "P2PFILESHARINGPROJ";

    public static byte[] create(int peerId) {

        ByteBuffer buffer = ByteBuffer.allocate(32);

        buffer.put(header.getBytes());
        buffer.put(new byte[10]);
        buffer.putInt(peerId);

        return buffer.array();
    }

    public static int extractPeerId(byte[] handshake) {

        ByteBuffer buffer = ByteBuffer.wrap(handshake);
        buffer.position(28);

        return buffer.getInt();
    }
}