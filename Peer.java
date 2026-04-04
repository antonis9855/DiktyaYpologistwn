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
            System.out.println("\n--- MENU ---");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Logout");
            System.out.println("4. Exit");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            String msgToSend = "";
            String tempUsername = "";

              switch (choice) {
                case "1": {
                    System.out.print("Username: ");
                    String uname = scanner.nextLine();
                    System.out.print("Password: ");
                    String pass = scanner.nextLine();
                    msgToSend = "REGISTER|" + uname + "|" + pass;
                    break;
                }
                case "2": {
                    if (myTokenId != null) {
                        System.out.println("You are already logged in!");
                        continue;
                    }
                    System.out.print("Username: ");
                    tempUsername = scanner.nextLine();
                    System.out.print("Password: ");
                    String pass = scanner.nextLine();
                    msgToSend = "LOGIN|" + tempUsername + "|" + pass + "|" + myPort;
                    break;
                }
                case "3":
                    if (myTokenId == null) {
                        System.out.println("You must login first!");
                        continue;
                    }
                    msgToSend = "LOGOUT|" + myTokenId;
                    break;
                case "4":
                    break label;
                default:
                    System.out.println("Invalid option. Try again.");
                    continue;
            }

           out.println(msgToSend);

           String response = in.readLine();
           if (response == null) {
                System.out.println("Server closed the connection.");
                break;
            }

            String[] responseParts = response.split("\\|");
            String status = responseParts[0];
            String info = responseParts.length > 1 ? responseParts[1] : "";

            System.out.println("[SERVER] " + status + (info.isEmpty() ? "" : ": " + info));

            if (choice.equals("2") && status.equals("SUCCESS")) {
                myTokenId = info; // Αποθήκευση του token
                myUsername = tempUsername; // Αποθήκευση του username

                System.out.println(">> Login successful! Token ID: " + myTokenId);
                
                Items generator = new Items(myUsername);
               
                generatorThread = new Thread(generator);
               
                generatorThread.start();

                System.out.println(">> Item Generator started in background!");
            }
            if (choice.equals("3") && status.equals("SUCCESS")) {
                myTokenId = null;
                myUsername = null;
                if (generatorThread != null) {
                    generatorThread.interrupt();
                    generatorThread = null;
                    System.out.println(">> Item Generator stopped.");
                }
            }
        }
        serverSocket.close();
        System.exit(0);
    }
}
