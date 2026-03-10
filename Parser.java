import java.io.*;
import java.util.*;

public class Parser {
    
    public static Properties parseCommon(String filePath) throws Exception {
        Properties config = new Properties();
        config.load(new FileReader(filePath));
        return config;
    }
    public static void main(String[] args) throws Exception {
        Properties config = parseCommon("Common.cfg");

        int numberPreferredNeighbors = Integer.parseInt(config.getProperty("NumberOfPreferredNeighbors"));
        int unchokingInterval = Integer.parseInt(config.getProperty("UnchokingInterval"));
        int optimisticUnchokingInterval = Integer.parseInt(config.getProperty("OptimisticUnchokingInterval"));
        String fileName = config.getProperty("FileName");
        long fileSize = Long.parseLong(config.getProperty("FileSize"));
        int pieceSize = Integer.parseInt(config.getProperty("PieceSize"));

        System.out.println(numberPreferredNeighbors);
        System.out.println(unchokingInterval);
        System.out.println(optimisticUnchokingInterval);
        System.out.println(fileName);
        System.out.println(fileSize);
    }
}