package flopbot.data.cache;

import org.bson.types.ObjectId;

public class Stake {
    private ObjectId id;
    private long userId;
    private String txid;
    private int vout;
    private String rewardWallet;
    private double amount;
    private long startTime;
    private long lastClaim;

    // Default no-args constructor
    public Stake() {
    }

    public Stake(long userId, String txid, int vout, String rewardWallet, double amount) {
        id = new ObjectId();
        this.userId = userId;
        this.txid = txid;
        this.vout = vout;
        this.amount = amount;
        this.startTime = System.currentTimeMillis();
        this.lastClaim = System.currentTimeMillis();
    }

    public long getStartTime() {
        return startTime;
    }
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    // Getters and Setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getTxid() {
        return txid;
    }

    public void setTxid(String txid) {
        this.txid = txid;
    }

    public int getVout() {
        return vout;
    }

    public void setVout(int vout) {
        this.vout = vout;
    }

    public String getRewardWallet() {
        return rewardWallet;
    }

    public void setRewardWallet(String rewardWallet) {
        this.rewardWallet = rewardWallet;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getLastClaim() {
        return lastClaim;
    }

    public void setLastClaim(long lastClaim) {
        this.lastClaim = lastClaim;
    }
}
