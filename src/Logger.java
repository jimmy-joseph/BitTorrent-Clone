import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    static final Path LOG_DIR = Paths.get("logs");

    static PrintWriter writer;

    public static void init(int peerId) throws Exception {
        Files.createDirectories(LOG_DIR);
        writer = new PrintWriter(new FileWriter(LOG_DIR.resolve("log_peer_" + peerId + ".log").toFile()), true);
    }

    public static synchronized void log(String msg) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        if (writer != null) {
            writer.println(time + ": " + msg);
        }
        System.out.println(time + ": " + msg);
    }
}
