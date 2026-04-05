import java.io.*;
import java.net.*;
import java.util.Random;

public class Bidder implements Runnable {
    private String username;
    private String tokenId;
    private String serverIp;
    private int serverPort;
    private Random random;

    public Bidder(String username, String tokenId, String serverIp, int serverPort) {
        this.username = username;
        this.tokenId = tokenId;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.random = new Random();
    }

    @Override
    public void run() {
        try {
            while (true) {
                Thread.sleep(10000);

                try (Socket socket = new Socket(serverIp, serverPort);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    out.println("GET_CURRENT_AUCTION|" + tokenId);
                    String response = in.readLine();

                    if (response != null && response.startsWith("SUCCESS")) {
                        String[] parts = response.split("\\|");
                        String currentObjectId = parts[1];
                        int highestBid = Integer.parseInt(parts[2]);

                        if (random.nextDouble() <= 0.60) {

                            double randValue = random.nextDouble();
                            int newBid = (int) (highestBid * (1.0 + (randValue / 10.0)));

                            if (newBid <= highestBid) {
                                newBid = highestBid + 1;
                            }

                            System.out.println("\n Buyer : " + username + "wants this : " + currentObjectId + "and offers:  " + newBid);

                            out.println("PLACE_BID|" + tokenId + "|" + newBid);
                            String bidResponse = in.readLine();
                            System.out.println("Server responded : " + bidResponse);
                        } else {
                            System.out.println("\n" + username + "Saw the object " + currentObjectId + " but not interested.");
                        }
                    }
                } catch (IOException e) {
                }
            }
        } catch (InterruptedException e) {
            System.out.println(">> Bidder thread stopped.");
        }
    }
}
