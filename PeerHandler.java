import java.io.*;
import java.net.*;

public class PeerHandler implements Runnable {

    Socket socket;
    BufferedReader in;
    PrintWriter out;

    public PeerHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[SERVER] Received: " + line);
                out.println("OK:" + line);
            }

        } catch (IOException e) {
            System.out.println("[SERVER] Peer disconnected: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException e) {}
        }
    }
}
