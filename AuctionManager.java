public class AuctionManager implements Runnable {

    public void run() {
        System.out.println("[SERVER] AuctionManager started.");
        while (true) {
            try {
                if (AuctionServer.currentAuctions.size() >= AuctionServer.MAX_AUCTIONS) {
                    Thread.sleep(1000);
                    continue;
                }

                AuctionItem item = AuctionServer.pollNextItem();

                if (item == null) {
                    Thread.sleep(2000);
                    continue;
                }

                if (!AuctionServer.activeSessions.containsKey(item.tokenId)) {
                    System.out.println("[SERVER] Seller offline, skipping " + item.objectId);
                    continue;
                }

                ActiveAuction auction = new ActiveAuction(item);
                AuctionServer.currentAuctions.put(item.objectId, auction);
                AuctionServer.currentAuction = auction;
                new Thread(() -> runAuction(auction)).start();

            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void runAuction(ActiveAuction auction) {
        AuctionItem item = auction.item;
        System.out.println("[SERVER] Auction started: " + item.objectId + " at " + item.startBid + " for " + item.auctionDuration + "s");
        boolean cancelled = false;
        int timeLeft = item.auctionDuration;
        try {
            while (timeLeft > 0) {
                Thread.sleep(1000);
                timeLeft--;
                if (!AuctionServer.activeSessions.containsKey(item.tokenId)) {
                    auction.isFinished = true;
                    System.out.println("[SERVER] Seller disconnected, auction cancelled: " + item.objectId);
                    AuctionServer.broadcastToAllPeers("AUCTION_CANCELLED|" + item.objectId);
                    cancelled = true;
                    break;
                }
            }

            if (!cancelled) {
                auction.isFinished = true;
                if (auction.highestBidderUsername != null) {
                    AuctionServer.addPendingWinner(auction.highestBidderTokenId, auction);
                    System.out.println("[SERVER] " + item.objectId + " offered to " + auction.highestBidderUsername + " for " + auction.currentBid);
                } else {
                    System.out.println("[SERVER] " + item.objectId + " ended with no bids.");
                }
            }
        } catch (InterruptedException e) {
        } finally {
            AuctionServer.currentAuctions.remove(item.objectId);
            if (AuctionServer.currentAuction == auction) AuctionServer.currentAuction = AuctionServer.getAnyAuction();
        }
    }
}
