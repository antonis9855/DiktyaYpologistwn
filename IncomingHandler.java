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

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[PEER] Received from peer: " + line);
                out.println("ACK:" + line);
            }

        } catch (IOException e) {
            System.out.println("[PEER] IncomingHandler error: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException e) {}
        }
    }
}
