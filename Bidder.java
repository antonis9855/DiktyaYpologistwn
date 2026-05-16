import java.io.*;
import java.net.*;
import java.util.Random;

public class Bidder implements Runnable {
    private String username;
    private String tokenId;
    private String serverIp;
    private int serverPort;
    private boolean simMode;
    private Random random = new Random();

    public Bidder(String username, String tokenId, String serverIp, int serverPort, boolean simMode) {
        this.username = username;
        this.tokenId = tokenId;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.simMode = simMode;
    }

    public void run() {
        try {
            while (true) {
                Thread.sleep(simMode ? 3000 : 10000);
                try (Socket socket = new Socket(serverIp, serverPort);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    out.println("CHECK_WINNER|" + tokenId);
                    String winnerResponse = in.readLine();
                    if (winnerResponse != null && winnerResponse.startsWith("SUCCESS")) {
                        String[] winParts = winnerResponse.split("\\|");
                        String wonObjectId = winParts[1];
                        String sellerIp = winParts[2];
                        int sellerPort = Integer.parseInt(winParts[3]);
                        System.out.println("[" + username + "] Won: " + wonObjectId + "! Starting UDP Go-Back-N...");
                        boolean ok = GoBackNReceiver.receiveFile(sellerIp, sellerPort, wonObjectId, username);
                        if (ok) out.println("TRANSACTION_COMPLETE|" + tokenId + "|" + wonObjectId);
                        in.readLine();
                    }

                    out.println("GET_CURRENT_AUCTION|" + tokenId);
                    String response = in.readLine();
                    if (response != null && response.startsWith("SUCCESS")) {
                        String[] parts = response.split("\\|");
                        String objectId = parts[1];
                        int highestBid = Integer.parseInt(parts[3]);
                        if (random.nextDouble() <= 0.60) {
                            int newBid = (int)(highestBid * (1.0 + random.nextDouble() / 10.0));
                            if (newBid <= highestBid) newBid = highestBid + 1;
                            System.out.println("[" + username + "] Bidding on " + objectId + ": " + newBid);
                            out.println("PLACE_BID|" + tokenId + "|" + newBid);
                            System.out.println("[" + username + "] " + in.readLine());
                        } else {
                            System.out.println("[" + username + "] Not interested in " + objectId);
                        }
                    }
                } catch (IOException e) {}
            }
        } catch (InterruptedException e) {
            System.out.println("[" + username + "] Bidder stopped.");
        }
    }
}
