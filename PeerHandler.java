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
                System.out.println("[SERVER] << " + line);
                String[] parts = line.split("\\|");
                switch (parts[0]) {
                    case "REGISTER":            register(parts);            break;
                    case "LOGIN":               login(parts);               break;
                    case "LOGOUT":              logout(parts);              break;
                    case "REQUEST_AUCTION":     requestAuction(parts);      break;
                    case "GET_CURRENT_AUCTION": getCurrentAuction(parts);   break;
                    case "GET_AUCTION_DETAILS": getAuctionDetails(parts);   break;
                    case "PLACE_BID":           placeBid(parts);            break;
                    case "CHECK_WINNER":        checkWinner(parts);         break;
                    case "TRANSACTION_COMPLETE": transactionComplete(parts); break;
                    default: out.println("ERROR|Unknown command");
                }
            }
        } catch (IOException e) {
            System.out.println("[SERVER] Peer disconnected.");
        } finally {
            try { socket.close(); } catch (IOException e) {}
        }
    }

    private void register(String[] parts) {
        if (parts.length != 3) { out.println("ERROR|Bad format"); return; }
        String username = parts[1];
        String password = parts[2];
        if (!AuctionServer.accounts.containsKey(username)) {
            AuctionServer.accounts.put(username, new User(username, password));
            out.println("SUCCESS|Account created for " + username);
            System.out.println("[SERVER] Registered: " + username);
        } else {
            out.println("ERROR|Username taken");
        }
    }

    private void login(String[] parts) {
        if (parts.length != 4) { out.println("ERROR|Bad format"); return; }
        String username = parts[1];
        String password = parts[2];
        int port;
        try {
            port = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            out.println("ERROR|Bad port");
            return;
        }
        User user = AuctionServer.accounts.get(username);
        if (user != null && user.password.equals(password)) {
            String token = String.valueOf(new Random().nextInt(1000000000));
            String ipAddress = socket.getInetAddress().getHostAddress();
            AuctionServer.activeSessions.put(token, new Session(token, username, ipAddress, port));
            out.println("SUCCESS|" + token);
            System.out.println("[SERVER] Login: " + username + " token=" + token);
        } else {
            out.println("ERROR|Wrong credentials");
        }
    }

    private void logout(String[] parts) {
        if (parts.length != 2) { out.println("ERROR|Bad format"); return; }
        String token = parts[1];
        if (AuctionServer.activeSessions.remove(token) != null) {
            out.println("SUCCESS|Logged out");
            System.out.println("[SERVER] Logout: token=" + token);
        } else {
            out.println("ERROR|Invalid token");
        }
    }

    private void requestAuction(String[] parts) {
        if (parts.length != 6) { out.println("ERROR|Bad format"); return; }
        String token = parts[1];
        if (!AuctionServer.activeSessions.containsKey(token)) { out.println("ERROR|Invalid token"); return; }
        String objectId = parts[2];
        String description = parts[3];
        int startBid = Integer.parseInt(parts[4]);
        int duration = Integer.parseInt(parts[5]);
        AuctionServer.auctionQueue.add(new AuctionItem(token, objectId, description, startBid, duration));
        out.println("SUCCESS|" + objectId + " queued");
        System.out.println("[SERVER] Queued: " + objectId + " queue size=" + AuctionServer.auctionQueue.size());
    }

    private void getCurrentAuction(String[] parts) {
        if (parts.length != 2) { out.println("ERROR|Bad format"); return; }
        if (!AuctionServer.activeSessions.containsKey(parts[1])) { out.println("ERROR|Invalid token"); return; }
        ActiveAuction auction = AuctionServer.currentAuction;
        if (auction != null && !auction.isFinished) {
            out.println("SUCCESS|" + auction.item.objectId + "|" + auction.item.description + "|" + auction.currentBid);
        } else {
            out.println("ERROR|No active auction");
        }
    }

    private void getAuctionDetails(String[] parts) {
        if (parts.length != 2) { out.println("ERROR|Bad format"); return; }
        if (!AuctionServer.activeSessions.containsKey(parts[1])) { out.println("ERROR|Invalid token"); return; }
        ActiveAuction auction = AuctionServer.currentAuction;
        if (auction != null && !auction.isFinished) {
            out.println("SUCCESS|" + auction.item.objectId + "|" + auction.item.description + "|" + auction.currentBid + "|" + auction.getTimeRemainingSeconds());
        } else {
            out.println("ERROR|No active auction");
        }
    }

    private void placeBid(String[] parts) {
        if (parts.length != 3) { out.println("ERROR|Bad format"); return; }
        String token = parts[1];
        if (!AuctionServer.activeSessions.containsKey(token)) { out.println("ERROR|Invalid token"); return; }
        int newBid;
        try {
            newBid = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            out.println("ERROR|Bad bid");
            return;
        }
        ActiveAuction auction = AuctionServer.currentAuction;
        if (auction == null || auction.isFinished) { out.println("ERROR|No active auction"); return; }
        String broadcastMsg = null;
        synchronized (auction) {
            if (auction.item.tokenId.equals(token)) { out.println("ERROR|Cannot bid on own item"); return; }
            if (newBid > auction.currentBid) {
                auction.currentBid = newBid;
                auction.highestBidderTokenId = token;
                auction.highestBidderUsername = AuctionServer.activeSessions.get(token).username;
                out.println("SUCCESS|Bid accepted");
                System.out.println("[SERVER] New bid on " + auction.item.objectId + ": " + newBid + " by " + auction.highestBidderUsername);
                broadcastMsg = "BID_UPDATE|" + auction.item.objectId + "|" + newBid + "|" + auction.highestBidderUsername;
            } else {
                out.println("ERROR|Bid too low, current=" + auction.currentBid);
            }
        }
        if (broadcastMsg != null) AuctionServer.broadcastToAllPeers(broadcastMsg);
    }

    private void checkWinner(String[] parts) {
        if (parts.length != 2) { out.println("ERROR|Bad format"); return; }
        String token = parts[1];
        ActiveAuction wonAuction = null;
        synchronized (AuctionServer.completedAuctions) {
            for (ActiveAuction auction : AuctionServer.completedAuctions) {
                if (token.equals(auction.highestBidderTokenId)) { wonAuction = auction; break; }
            }
            if (wonAuction != null) AuctionServer.completedAuctions.remove(wonAuction);
        }
        if (wonAuction != null) {
            Session sellerSession = AuctionServer.activeSessions.get(wonAuction.item.tokenId);
            if (sellerSession != null) {
                out.println("SUCCESS|" + wonAuction.item.objectId + "|" + sellerSession.ipAddress + "|" + sellerSession.port);
            } else {
                out.println("ERROR|Seller offline");
            }
        } else {
            out.println("ERROR|No win");
        }
    }

    private void transactionComplete(String[] parts) {
        if (parts.length != 3) { out.println("ERROR|Bad format"); return; }
        if (!AuctionServer.activeSessions.containsKey(parts[1])) { out.println("ERROR|Invalid token"); return; }
        String buyerUsername = AuctionServer.activeSessions.get(parts[1]).username;
        String objectId = parts[2];
        System.out.println("[SERVER] Transaction: " + buyerUsername + " got " + objectId);
        out.println("SUCCESS|Transaction recorded");
    }
}
