public class Session {
    public String tokenId;
    public String username;
    public String ipAddress;
    public int port;

    public Session(String tokenId, String username, String ipAddress, int port) {
        this.tokenId = tokenId;
        this.username = username;
        this.ipAddress = ipAddress;
        this.port = port;
    }
}
