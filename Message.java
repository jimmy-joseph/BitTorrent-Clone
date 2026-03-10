public class Message {

    public static final byte BITFIELD = 5;

    public final byte type;
    public final byte[] payload;

    public Message(byte type) {
        this(type, new byte[0]);
    }

    public Message(byte type, byte[] payload) {
        this.type = type;
        this.payload = payload;
    }
}