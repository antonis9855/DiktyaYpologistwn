import java.io.*;
import java.net.*;
import java.util.*;

public class Peer {

    static final String SERVER_HOST = "127.0.0.1";
    static final int SERVER_PORT = 5000;

    static int myPort;
    static ServerSocket myServerSocket;

    public static void main(String[] args) throws IOException {
        myServerSocket = new ServerSocket(0);
        myPort = myServerSocket.getLocalPort();
        System.out.println("Peer listening on port " + myPort);

        Thread listenerThread = new Thread(new PeerListener(myServerSocket));
        listenerThread.setDaemon(true);
        listenerThread.start();

        Socket serverSocket = new Socket(SERVER_HOST, SERVER_PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
        PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);

        System.out.println("Connected to Auction Server");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Enter message: ");
            String msg = scanner.nextLine();
            if (msg.equals("exit")) break;
            out.println(msg);
            String response = in.readLine();
            System.out.println("[PEER] Server replied: " + response);
        }

        serverSocket.close();
    }
}
