import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class AuctionServer {

    static final int PORT = 5000;

    static Map<String, User> accounts = new ConcurrentHashMap<>();
    static Map<String, Session> activeSessions = new ConcurrentHashMap<>();
    static Queue<AuctionItem> auctionQueue = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Auction Server listening on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New peer connected: " + clientSocket.getInetAddress());
            Thread t = new Thread(new PeerHandler(clientSocket));
            t.start();
        }
    }
}
