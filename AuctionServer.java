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
    static Map<String, ActiveAuction> currentAuctions = new ConcurrentHashMap<>();
    static List<ActiveAuction> completedAuctions = Collections.synchronizedList(new ArrayList<>());
    static Map<String, Queue<ActiveAuction>> pendingWinners = new ConcurrentHashMap<>();
    static final double BETA = 0.25;
    static final int MAX_AUCTIONS = 2;

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

    static void updateReputation(String username, int result) {
        User user = accounts.get(username);
        if (user == null) return;
        user.reputation_score = (1.0 - BETA) * user.reputation_score + BETA * result;
        System.out.printf("[SERVER] Reputation %s = %.2f%n", username, user.reputation_score);
    }

    static AuctionItem pollNextItem() {
        AuctionItem first = auctionQueue.poll();
        if (first == null) return null;
        AuctionItem second = auctionQueue.poll();
        if (second == null) return first;
        double firstReputation = reputationOfSeller(first);
        double secondReputation = reputationOfSeller(second);
        if (firstReputation < secondReputation) {
            auctionQueue.add(first);
            return second;
        }
        auctionQueue.add(second);
        return first;
    }

    static double reputationOfSeller(AuctionItem item) {
        Session session = activeSessions.get(item.tokenId);
        if (session == null) return 0.0;
        User user = accounts.get(session.username);
        if (user == null) return 0.0;
        return user.reputation_score;
    }

    static ActiveAuction getAnyAuction() {
        List<ActiveAuction> active = new ArrayList<>();
        for (ActiveAuction auction : currentAuctions.values()) {
            if (!auction.isFinished) active.add(auction);
        }
        if (active.isEmpty()) return null;
        return active.get(new Random().nextInt(active.size()));
    }

    static ActiveAuction getAuction(String objectId) {
        ActiveAuction auction = currentAuctions.get(objectId);
        if (auction != null && !auction.isFinished) return auction;
        return null;
    }

    static void addPendingWinner(String token, ActiveAuction auction) {
        pendingWinners.computeIfAbsent(token, k -> new ConcurrentLinkedQueue<>()).add(auction);
    }

    static ActiveAuction peekPendingWinner(String token) {
        Queue<ActiveAuction> q = pendingWinners.get(token);
        if (q == null) return null;
        return q.peek();
    }

    static ActiveAuction removePendingWinner(String token, String objectId) {
        Queue<ActiveAuction> q = pendingWinners.get(token);
        if (q == null) return null;
        for (ActiveAuction auction : q) {
            if (auction.item.objectId.equals(objectId)) {
                q.remove(auction);
                if (q.isEmpty()) pendingWinners.remove(token);
                return auction;
            }
        }
        return null;
    }
}
