public class AuctionItem {
    public String tokenId;
    public String objectId;
    public String description;
    public int startBid;
    public int auctionDuration;
    public long arrivalTime;

    public AuctionItem(String tokenId, String objectId, String description, int startBid, int auctionDuration) {
        this.tokenId = tokenId;
        this.objectId = objectId;
        this.description = description;
        this.startBid = startBid;
        this.auctionDuration = auctionDuration;
        this.arrivalTime = System.currentTimeMillis();
    }
}
