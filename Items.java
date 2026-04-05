import java.io.*;
import java.util.Random;

public class Items implements Runnable {
    private String username;
    private String tokenId;
    private PrintWriter out;
    private File sharedDir;
    private int objectCounter = 1;
    private Random random;

    public Items (String username, String tokenId, PrintWriter out){
        this.username = username;
        this.tokenId = tokenId;
        this.out = out;
        this.random = new Random();
        this.sharedDir = new File("shared_directory_" + username);
        if (!this.sharedDir.exists()) {
            this.sharedDir.mkdir();
        }
    }
    @Override
    public void run() {
        try {
            while (true) {
                double randValue = random.nextDouble();
                double waitTimeSeconds = randValue * 120;
                long waitTimeMillis = (long) (waitTimeSeconds * 1000);

                System.out.println("\n[" + username + "Next item in " + (int) waitTimeSeconds + " seconds..");

                Thread.sleep(waitTimeMillis);

                generateObjectFile();
            }
        } catch (InterruptedException e) {
                    System.out.println(" ");
                }
        }

    private void generateObjectFile() {
        String objectId = "Object_" + String.format("%02d", objectCounter);
        String fileName = objectId + ".txt";
        File objectFile = new File(sharedDir, fileName);

        int startBid = 10 + random.nextInt(91);
        int auctionDuration = 30 + random.nextInt(91);

        String content = "[object_id: " + objectId +
                "; description: \"a description for " + objectId + "\"" +
                "; start_bid: \"" + startBid + "\"" +
                "; auction_duration: \"" + auctionDuration + "\"]";

        try (PrintWriter writer = new PrintWriter(new FileWriter(objectFile))) {
            writer.println(content);
            System.out.println("\n[" + username + "New file: " + fileName + " in file " + sharedDir.getName());
            objectCounter++;
            String requestMsg = "REQUEST_AUCTION|" + tokenId + "|" + objectId + "|a description for " + objectId + "|" + startBid + "|" + auctionDuration;
            out.println(requestMsg);
        } catch (IOException e) {
            System.out.println("Failed to create file: " + e.getMessage());
        }
    }
}
