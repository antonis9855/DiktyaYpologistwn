import java.io.*;
import java.net.*;
import java.util.Random;

public class Items implements Runnable {
    private String username;
    private String tokenId;
    private String serverHost;
    private int serverPort;
    private boolean simMode;
    private File sharedDir;
    private int objectCounter = 1;
    private Random random = new Random();

    public Items(String username, String tokenId, String serverHost, int serverPort, boolean simMode) {
        this.username = username;
        this.tokenId = tokenId;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.simMode = simMode;
        this.sharedDir = new File("shared_directory_" + username);
        this.sharedDir.mkdir();
    }

    public void run() {
        try {
            while (true) {
                int waitSeconds = (int)(random.nextDouble() * (simMode ? 15 : 120));
                System.out.println("[" + username + "] Next item in " + waitSeconds + "s");
                Thread.sleep(waitSeconds * 1000L);
                generateItem();
            }
        } catch (InterruptedException e) {
            System.out.println("[" + username + "] Generator stopped.");
        }
    }

    private void generateItem() {
        String objectId = username + "Object_" + String.format("%02d", objectCounter);
        int startBid = 10 + random.nextInt(91);
        int duration = simMode ? 10 + random.nextInt(11) : 30 + random.nextInt(91);
        String content = "[object_id: " + objectId + "; description: \"item by " + username + "\"; start_bid: \"" + startBid + "\"; auction_duration: \"" + duration + "\"]";

        File file = new File(sharedDir, objectId + ".txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println(content);
            System.out.println("[" + username + "] Created " + objectId + " bid=" + startBid + " dur=" + duration + "s");
            objectCounter++;
        } catch (IOException e) {
            System.out.println("[" + username + "] Failed to create file.");
            return;
        }

        try (Socket socket = new Socket(serverHost, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.println("REQUEST_AUCTION|" + tokenId + "|" + objectId + "|item by " + username + "|" + startBid + "|" + duration);
            System.out.println("[" + username + "] Server: " + in.readLine());
        } catch (IOException e) {
            System.out.println("[" + username + "] Failed to queue item.");
        }
    }
}
