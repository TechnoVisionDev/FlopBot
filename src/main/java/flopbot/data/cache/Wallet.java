package flopbot.data.cache;

/**
 * POJO that represents a user's flopcoin wallet.
 *
 * @author TechnoVision
 */
public class Wallet {

    private Long user;
    private Long balance;

    public Wallet() { }

    public Wallet(Long user) {
        this.user = user;
        this.balance = 0L;
    }

    public Wallet(Long user, Long balance) {
        this.user = user;
        this.balance = balance;
    }

    public Long getUser() {
        return user;
    }

    public void setUser(Long user) {
        this.user = user;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }
}
