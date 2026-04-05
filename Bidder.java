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
                        out.println("CHECK_WINNER|" + tokenId);
                        String winnerResponse = in.readLine();

                        if (winnerResponse != null && winnerResponse.startsWith("SUCCESS")) {
                            String[] winParts = winnerResponse.split("\\|");
                            String wonObjectId = winParts[1];
                            String sellerIp = winParts[2];
                            int sellerPort = Integer.parseInt(winParts[3]);

                            System.out.println("\n [" + username + "-Buyer] Congrats! You won :  " + wonObjectId + "!");
                            downloadFileFromSeller(sellerIp, sellerPort, wonObjectId);
                            out.println("TRANSACTION_COMPLETE|" + tokenId + "|" + wonObjectId);
                        }
                        if (random.nextDouble() <= 0.60) {
                            double randValue = random.nextDouble();
                            int newBid = (int) (highestBid * (1.0 + (randValue / 10.0)));
                            if (newBid <= highestBid) {
                                newBid = highestBid + 1;
                            }
                            System.out.println("\n[" + username + "-Buyer] I want : " + currentObjectId + "and i bid: " + newBid);
                            out.println("PLACE_BID|" + tokenId + "|" + newBid);
                            String bidResponse = in.readLine();
                            System.out.println("[" + username + "-Buyer] Server responded " + bidResponse);
                        } else {
                            System.out.println("\n[" + username + "-Buyer] I saw : " + currentObjectId + " But not interested.");
                        }
                    }
                } catch (IOException e) {
                }
            }

        } catch (InterruptedException e) {
            System.out.println(">> Bidder thread stopped.");
        }
    }
    private void downloadFileFromSeller(String ip, int port, String objectId) {
        File buyerDir = new File("shared_directory_" + username);
        if (!buyerDir.exists()) buyerDir.mkdir();

        File newFile = new File(buyerDir, objectId + ".txt");

        try (Socket p2pSocket = new Socket(ip, port);
             PrintWriter p2pOut = new PrintWriter(p2pSocket.getOutputStream(), true);
             BufferedReader p2pIn = new BufferedReader(new InputStreamReader(p2pSocket.getInputStream()));
             FileWriter fileWriter = new FileWriter(newFile)) {

            p2pOut.println("DOWNLOAD|" + objectId);
            System.out.println("[" + username + "-Buyer] Downloading file " + objectId + ".txt...");
            String line;
            while ((line = p2pIn.readLine()) != null) {
                if (line.equals("EOF")) break;
                fileWriter.write(line + "\n");
            }
            System.out.println("[" + username + "-Buyer] Saving file completed!");
        } catch (IOException e) {
            System.out.println("[" + username + "-Buyer] Failed to connect P2P: " + e.getMessage());
        }
    }
}
