import java.io.*;
import java.net.*;

public class PeerListener implements Runnable {

    ServerSocket serverSocket;

    public PeerListener(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void run() {
        System.out.println("[PEER] Listening on port " + serverSocket.getLocalPort());
        while (true) {
            try {
                Socket incoming = serverSocket.accept();
                new Thread(new IncomingHandler(incoming)).start();
            } catch (IOException e) {
                break;
            }
        }
    }
}
