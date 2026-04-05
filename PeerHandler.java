import java.io.*;
import java.net.*;
import java.util.Random;

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
                
                String[] pieces = line.split("\\|");
                String command = pieces[0];
                
                switch (command){
                    case "REGISTER":
                        register (pieces);
                        break;
                    case "LOGIN":
                        login (pieces);
                        break;
                    case "LOGOUT":
                        logout (pieces);
                        break;
                    case "REQUEST_AUCTION":
                        requestAuction(pieces);
                        break;
                    case "GET_CURRENT_AUCTION":
                        getCurrentAuction(pieces);
                        break;
                    case "GET_AUCTION_DETAILS":
                        getAuctionDetails(pieces);
                        break;
                    case "PLACE_BID":
                        placeBid(pieces);
                        break;
                    case "CHECK_WINNER":
                        checkWinner(pieces);
                        break;
                    default:
                        out.println("Error command");
                }           
            }
        } catch (IOException e) {
            System.out.println("[SERVER] Peer disconnected: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException e) {}
        }
    }

   private void register(String[] pieces) {
        if (pieces.length != 3) {
            out.println("ERROR|Format is REGISTER|username|password");
            return;
        }
        
        String username = pieces[1];
        String password = pieces[2];

       if (!AuctionServer.accounts.containsKey(username)) {
            AuctionServer.accounts.put(username, new User(username, password));
            out.println("SUCCESS|Account created for " + username);
        } else {
           out.println("ERROR|Username already exists. Please choose a different username.");
        }
    }

   private void login(String[] pieces) {
        if (pieces.length != 4) {
            out.println("ERROR|Format is LOGIN|username|password|port");
            return;
        }
        String username = pieces[1];
        String password = pieces[2];
        int peerPort;
        try {
            peerPort = Integer.parseInt(pieces[3]);
        } catch (NumberFormatException e) {
            out.println("ERROR|Invalid port number.");
            return;
        }
        User user = AuctionServer.accounts.get(username);
      
        if (user != null && user.password.equals(password)) {
            Random random = new Random();
            int randomNum = random.nextInt(1000000000);
            String tokenId = String.valueOf(randomNum);
            String ipAddress = socket.getInetAddress().getHostAddress();
            AuctionServer.activeSessions.put(tokenId, new Session(tokenId, username, ipAddress, peerPort));
            out.println("SUCCESS|" + tokenId);
        } else {
            out.println("ERROR|Invalid username or password.");
        }
    }

   private void logout(String[] pieces) {
       if (pieces.length != 2) {
           out.println("ERROR|Format is LOGOUT|tokenId");
           return;
        }
        String tokenId = pieces[1];

       if (AuctionServer.activeSessions.containsKey(tokenId)) {
           AuctionServer.activeSessions.remove(tokenId);
           out.println("SUCCESS|Logged out successfully.");
        } else {
           out.println("ERROR|Invalid Token ID.");
        }
    }
    private void requestAuction(String[] pieces) {
        if (pieces.length != 6) {
            out.println("ERROR|Format is REQUEST_AUCTION|tokenId|objectId|description|startBid|duration");
            return;
        }
        String tokenId = pieces[1];
        if (!AuctionServer.activeSessions.containsKey(tokenId)) {
            out.println("ERROR|Invalid or expired Token ID.");
            return;
        }

        String objectId = pieces[2];
        String description = pieces[3];
        int startBid = Integer.parseInt(pieces[4]);
        int duration = Integer.parseInt(pieces[5]);

        AuctionItem newItem = new AuctionItem(tokenId, objectId, description, startBid, duration);
        AuctionServer.auctionQueue.add(newItem);

        out.println("SUCCESS|Item " + objectId + " added to the auction queue!");
        System.out.println("[SERVER] Queue size is now: " + AuctionServer.auctionQueue.size());
    }
private void getCurrentAuction(String[] pieces) {
        if (pieces.length != 2) {
            out.println("ERROR|Format is GET_CURRENT_AUCTION|tokenId");
            return;
        }
        String tokenId = pieces[1];
        if (!AuctionServer.activeSessions.containsKey(tokenId)) {
            out.println("ERROR|Invalid Token ID");
            return;
        }
        ActiveAuction auction = AuctionServer.currentAuction;
        if (auction != null && !auction.isFinished) {
            out.println("SUCCESS|" + auction.item.objectId + "|" + auction.currentBid);
        } else {
            out.println("ERROR|No active auction right now");
        }
    }

    private void getAuctionDetails(String[] pieces) {
        
        if (pieces.length != 2) {
            out.println("ERROR|Format is GET_AUCTION_DETAILS|tokenId");
            return;
        }
        String tokenId = pieces[1];
        if (!AuctionServer.activeSessions.containsKey(tokenId)) {
            out.println("ERROR|Invalid Token ID");
            return;
        }
        ActiveAuction auction = AuctionServer.currentAuction;
        if (auction != null && !auction.isFinished) {
            out.println("SUCCESS|" + auction.item.objectId + "|" + auction.item.description + "|" + auction.item.startBid + "|" + auction.currentBid);
        } else {
            out.println("ERROR|No active auction");
        }
    }

    private void placeBid(String[] pieces) {
        if (pieces.length != 3) {
            out.println("ERROR|Format is PLACE_BID|tokenId|newBid");
            return;
        }
        String tokenId = pieces[1];
        if (!AuctionServer.activeSessions.containsKey(tokenId)) {
            out.println("ERROR|Invalid Token ID");
            return;
        }
        int newBid;
        try {
            newBid = Integer.parseInt(pieces[2]);
        } catch (NumberFormatException e) {
            out.println("ERROR|Invalid bid amount");
            return;
        }
        ActiveAuction auction = AuctionServer.currentAuction;
        if (auction != null && !auction.isFinished) {
           synchronized (auction) {
                if (auction.item.tokenId.equals(tokenId)) {
                    out.println("ERROR|You cannot bid on your own item!");
                    return;
                }
                if (newBid > auction.currentBid) {
                    auction.currentBid = newBid;
                    auction.highestBidderTokenId = tokenId;
                    auction.highestBidderUsername = AuctionServer.activeSessions.get(tokenId).username;
                    out.println("SUCCESS|Bid accepted");
                    System.out.println("[SERVER] New offer for " + auction.item.objectId + " from " + auction.highestBidderUsername + " with bid: " + newBid);
                } else {
                    out.println("ERROR|Bid too low. Current bid is " + auction.currentBid);
                }
            }
        } else {
            out.println("ERROR|No active auction to bid on");
        }
    }
   private void checkWinner(String[] pieces) {
        // Μορφή: CHECK_WINNER|tokenId
        if (pieces.length != 2) return;
        String tokenId = pieces[1];

        ActiveAuction wonAuction = null;

        // Ψάχνουμε στη λίστα αν υπάρχει δημοπρασία που κέρδισε αυτό το tokenId
        synchronized(AuctionServer.completedAuctions) {
            for (ActiveAuction a : AuctionServer.completedAuctions) {
                if (tokenId.equals(a.highestBidderTokenId)) {
                    wonAuction = a;
                    break;
                }
            }
            // Αν βρήκαμε ότι κέρδισε, το βγάζουμε από τη λίστα για να μην τον ξαναειδοποιήσουμε
            if (wonAuction != null) {
                AuctionServer.completedAuctions.remove(wonAuction);
            }
        }

        if (wonAuction != null) {
            String sellerToken = wonAuction.item.tokenId;
            Session sellerSession = AuctionServer.activeSessions.get(sellerToken);

            if (sellerSession != null) {
                // Του στέλνουμε το SUCCESS μαζί με το Όνομα Αρχείου, την IP και το Port του Πωλητή
                out.println("SUCCESS|" + wonAuction.item.objectId + "|" + sellerSession.ipAddress + "|" + sellerSession.port);
            } else {
                out.println("ERROR|Saler is offline.");
            }
        } else {
            out.println("ERROR|You haven't won any auction.");
        }
    }
}
