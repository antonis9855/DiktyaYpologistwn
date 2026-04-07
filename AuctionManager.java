public class AuctionManager implements Runnable {

    public void run() {
        System.out.println("[SERVER] AuctionManager started.");
        while (true) {
            try {
                AuctionItem item = AuctionServer.auctionQueue.poll();

                if (item == null) {
                    Thread.sleep(2000);
                    continue;
                }

                if (!AuctionServer.activeSessions.containsKey(item.tokenId)) {
                    System.out.println("[SERVER] Seller offline, skipping " + item.objectId);
                    continue;
                }

                AuctionServer.currentAuction = new ActiveAuction(item);
                System.out.println("[SERVER] Auction started: " + item.objectId + " at " + item.startBid + " for " + item.auctionDuration + "s");

                boolean cancelled = false;
                int timeLeft = item.auctionDuration;

                while (timeLeft > 0) {
                    Thread.sleep(1000);
                    timeLeft--;
                    if (!AuctionServer.activeSessions.containsKey(item.tokenId)) {
                        AuctionServer.currentAuction.isFinished = true;
                        System.out.println("[SERVER] Seller disconnected, auction cancelled: " + item.objectId);
                        AuctionServer.broadcastToAllPeers("AUCTION_CANCELLED|" + item.objectId);
                        cancelled = true;
                        break;
                    }
                }

                if (!cancelled) {
                    AuctionServer.currentAuction.isFinished = true;
                    ActiveAuction auction = AuctionServer.currentAuction;

                    if (auction.highestBidderUsername != null) {
                        System.out.println("[SERVER] " + item.objectId + " sold to " + auction.highestBidderUsername + " for " + auction.currentBid);
                        AuctionServer.completedAuctions.add(auction);

                        Session sellerSession = AuctionServer.activeSessions.get(item.tokenId);
                        if (sellerSession != null) {
                            User sellerUser = AuctionServer.accounts.get(sellerSession.username);
                            if (sellerUser != null) sellerUser.num_auctions_seller++;
                        }
                        User buyerUser = AuctionServer.accounts.get(auction.highestBidderUsername);
                        if (buyerUser != null) buyerUser.num_auctions_bidder++;
                    } else {
                        System.out.println("[SERVER] " + item.objectId + " ended with no bids.");
                    }
                }

                AuctionServer.currentAuction = null;

            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
