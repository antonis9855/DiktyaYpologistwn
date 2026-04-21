import java.io.*;
import java.net.*;
import java.util.*;

public class Peer {

    static final String SERVER_HOST = "127.0.0.1";
    static final int SERVER_PORT = 5000;

    static int myPort;
    static ServerSocket myServerSocket;
    static String myTokenId = null;
    static String myUsername = null;
    static Thread generatorThread = null;
    static Thread bidderThread = null;

    public static void main(String[] args) throws IOException {
        myServerSocket = new ServerSocket(0);
        myPort = myServerSocket.getLocalPort();
        Thread listener = new Thread(new PeerListener(myServerSocket));
        listener.setDaemon(true);
        listener.start();

        if (args.length >= 2) {
            boolean simMode = args.length >= 3 && args[2].equals("sim");
            autoMode(args[0], args[1], simMode);
            return;
        }

        Socket serverSocket = new Socket(SERVER_HOST, SERVER_PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
        PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);
        Scanner scanner = new Scanner(System.in);

        outer:
        while (true) {
            System.out.println("\n1.Register \n2.Login \n3.Logout \n4.Exit\n");
            System.out.print("> ");
            String choice = scanner.nextLine();
            String message = "";
            String tempUsername = "";

            switch (choice) {
                case "1":
                    System.out.print("Username: ");
                    String username = scanner.nextLine();
                    System.out.print("Password: ");
                    String password = scanner.nextLine();
                    message = "REGISTER|" + username + "|" + password;
                    break;
                case "2":
                    if (myTokenId != null) { System.out.println("Already logged in."); continue; }
                    System.out.print("Username: ");
                    tempUsername = scanner.nextLine();
                    System.out.print("Password: ");
                    String pass = scanner.nextLine();
                    message = "LOGIN|" + tempUsername + "|" + pass + "|" + myPort;
                    break;
                case "3":
                    if (myTokenId == null) { System.out.println("Not logged in."); continue; }
                    message = "LOGOUT|" + myTokenId;
                    break;
                case "4":
                    if (myTokenId != null) {
                        out.println("LOGOUT|" + myTokenId);
                        stopThreads();
                    }
                default:
                    continue;
            }

            out.println(message);
            String response = in.readLine();
            if (response == null) break;
            String[] parts = response.split("\\|");
            System.out.println("[SERVER] " + response);

            if (choice.equals("2") && parts[0].equals("SUCCESS")) {
                myTokenId = parts[1];
                myUsername = tempUsername;
                startThreads(false);
            }
            if (choice.equals("3") && parts[0].equals("SUCCESS")) {
                myTokenId = null;
                myUsername = null;
                stopThreads();
            }
        }
        serverSocket.close();
    }

    static void autoMode(String username, String password, boolean simMode) {
        System.out.println("[" + username + "] Starting...");
        try (Socket serverSocket = new Socket(SERVER_HOST, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
             PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true)) {

            out.println("REGISTER|" + username + "|" + password);
            System.out.println("[" + username + "] Register: " + in.readLine());

            out.println("LOGIN|" + username + "|" + password + "|" + myPort);
            String loginResponse = in.readLine();
            System.out.println("[" + username + "] Login: " + loginResponse);

            if (loginResponse == null || !loginResponse.startsWith("SUCCESS")) return;
            myTokenId = loginResponse.split("\\|")[1];
            myUsername = username;
        } catch (IOException e) {
            System.out.println("[" + username + "] Connection failed: " + e.getMessage());
            return;
        }

        startThreads(simMode);
        try { Thread.currentThread().join(); } catch (InterruptedException e) {}
    }

    static void startThreads(boolean simMode) {
        generatorThread = new Thread(new Items(myUsername, myTokenId, SERVER_HOST, SERVER_PORT, simMode));
        bidderThread = new Thread(new Bidder(myUsername, myTokenId, SERVER_HOST, SERVER_PORT, simMode));
        generatorThread.start();
        bidderThread.start();
        System.out.println("[" + myUsername + "] Threads started.");
    }

    static void stopThreads() {
        if (generatorThread != null) { generatorThread.interrupt(); generatorThread = null; }
        if (bidderThread != null) { bidderThread.interrupt(); bidderThread = null; }
    }
}
