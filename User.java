
public class User {
  public String username;
  public String password;
  public int num_auctions_seller = 0;
  public int num_auctions_bidder = 0;

  public User(String username, String password) {
    this.username = username;
    this.password = password;
  }
} 
