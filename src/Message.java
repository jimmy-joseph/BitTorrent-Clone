public class Message {

    public static final byte CHOKE = 0;
    public static final byte UNCHOKE = 1;
    public static final byte INTERESTED = 2;
    public static final byte NOT_INTERESTED = 3;
    public static final byte HAVE = 4;
    public static final byte BITFIELD = 5;
    public static final byte REQUEST = 6;
    public static final byte PIECE = 7;

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
