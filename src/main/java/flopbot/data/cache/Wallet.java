package flopbot.data.cache;

/**
 * POJO that represents a user's flopcoin wallet.
 *
 * @author TechnoVision
 */
public class Wallet {

    private Long user;
    private String address;

    public Wallet() { }

    public Wallet(Long user, String address) {
        this.user = user;
        this.address = address;
    }

    public Long getUser() {
        return user;
    }

    public void setUser(Long user) {
        this.user = user;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
