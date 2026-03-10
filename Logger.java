import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    static PrintWriter writer;

    public static void init(int peerId) throws Exception {

        writer = new PrintWriter(
                new FileWriter("log_peer_" + peerId + ".log"), true);
    }

    public static void log(String msg) {

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date());

        writer.println(time + ": " + msg);

        System.out.println(time + ": " + msg);
    }
}