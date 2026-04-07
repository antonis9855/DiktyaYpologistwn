import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class AuctionServer {

    static final int PORT = 5000;

    static Map<String, User> accounts = new ConcurrentHashMap<>();
    static Map<String, Session> activeSessions = new ConcurrentHashMap<>();
    static Queue<AuctionItem> auctionQueue = new ConcurrentLinkedQueue<>();
    static volatile ActiveAuction currentAuction = null;
    static List<ActiveAuction> completedAuctions = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Auction Server listening on port " + PORT);
        new Thread(new AuctionManager()).start();
        while (true) {
            Socket client = serverSocket.accept();
            new Thread(new PeerHandler(client)).start();
        }
    }

    static void broadcastToAllPeers(String message) {
        for (Session s : activeSessions.values()) {
            try (Socket sock = new Socket(s.ipAddress, s.port);
                 PrintWriter pw = new PrintWriter(sock.getOutputStream(), true)) {
                pw.println(message);
            } catch (IOException e) {}
        }
    }
}
