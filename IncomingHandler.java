import java.io.*;
import java.net.*;

public class IncomingHandler implements Runnable {

    Socket socket;

    public IncomingHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            String line = in.readLine();
            if (line == null) return;
            String[] parts = line.split("\\|");
            if (parts[0].equals("DOWNLOAD") && parts.length == 2) {
                sendFile(parts[1], out);
            } else if (parts[0].equals("BID_UPDATE") && parts.length == 4) {
                System.out.println("[" + Peer.myUsername + "] BID UPDATE: " + parts[1] + " -> " + parts[2] + " by " + parts[3]);
            } else if (parts[0].equals("AUCTION_CANCELLED") && parts.length == 2) {
                System.out.println("[" + Peer.myUsername + "] AUCTION CANCELLED: " + parts[1]);
            }
        } catch (IOException e) {
        } finally {
            try { socket.close(); } catch (IOException e) {}
        }
    }

    private void sendFile(String objectId, PrintWriter out) {
        String username = Peer.myUsername;
        if (username == null) { out.println("EOF"); return; }
        File file = new File("shared_directory_" + username, objectId + ".txt");
        if (!file.exists()) { out.println("EOF"); return; }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) out.println(line);
            out.println("EOF");
            System.out.println("[" + username + "] Sent file: " + objectId);
        } catch (IOException e) {
            out.println("EOF");
        }
        file.delete();
        System.out.println("[" + username + "] Deleted from shared_directory: " + objectId);
    }
}
