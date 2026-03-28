import java.io.*;
import java.net.*;

public class PeerListener implements Runnable {

    ServerSocket serverSocket;

    public PeerListener(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void run() {
        System.out.println("PeerListener waiting for incoming connections on port " + serverSocket.getLocalPort());
        while (true) {
            try {
                Socket incoming = serverSocket.accept();
                System.out.println("[PEER] Incoming connection from: " + incoming.getInetAddress());
                Thread t = new Thread(new IncomingHandler(incoming));
                t.start();
            } catch (IOException e) {
                System.out.println("[PEER] Listener error: " + e.getMessage());
                break;
            }
        }
    }
}
