public class ActiveAuction {
    public AuctionItem item;
    public int currentBid;
    public String highestBidderUsername;
    public String highestBidderTokenId;
    public volatile boolean isFinished;
    public long endTime;

    public ActiveAuction(AuctionItem item) {
        this.item = item;
        this.currentBid = item.startBid;
        this.highestBidderUsername = null;
        this.highestBidderTokenId = null;
        this.isFinished = false;
        this.endTime = System.currentTimeMillis() + (item.auctionDuration * 1000L);
    }

    public long getTimeRemainingSeconds() {
        return Math.max(0, (endTime - System.currentTimeMillis()) / 1000);
    }
}
