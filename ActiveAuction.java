public class ActiveAuction {
    public AuctionItem item;
    public int currentBid;
    public String highestBidderUsername;
    public String highestBidderTokenId;
    public boolean isFinished;

    public ActiveAuction(AuctionItem item) {
        this.item = item;
        this.currentBid = item.startBid;
        this.highestBidderUsername = null; 
        this.highestBidderTokenId = null;
        this.isFinished = false;
    }
}
