package flopbot.handlers;

import com.google.gson.Gson;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import flopbot.FlopBot;
import flopbot.data.cache.Wallet;
import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;

import static flopbot.handlers.StatsHandler.COIN_SYMBOL;

public class WalletHandler {

    private final OkHttpClient client;
    private final Gson gson;
    private final FlopBot bot;

    // Caching fields for coin rate
    private double cachedRate = 0.0;
    private long lastRateUpdate = 0;
    // 10 minutes in milliseconds
    private static final long RATE_UPDATE_INTERVAL = 10 * 60 * 1000;

    public WalletHandler(FlopBot bot) {
        this.bot = bot;
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    private Wallet getWallet(long userID) {
        Wallet wallet = bot.database.wallets.find(Filters.eq("user", userID)).first();
        if (wallet == null) {
            return createWallet(userID);
        }
        return wallet;
    }

    private Wallet createWallet(long userID) {
        Wallet wallet = new Wallet(userID);
        bot.database.wallets.insertOne(wallet);
        return wallet;
    }

    public long getBalance(long userID) {
        return getWallet(userID).getBalance();
    }

    public void sendCoins(long userID, long amount) {
        getWallet(userID);
        bot.database.wallets.updateOne(
                Filters.eq("user", userID),
                Updates.inc("balance", amount)
        );
    }

    public void setBalance(long userID, long newBalance) {
        getWallet(userID);
        bot.database.wallets.updateOne(
                Filters.eq("user", userID),
                Updates.set("balance", newBalance)
        );
    }

    /**
     * Returns the wallet balance in dollars.
     * Updates the coin rate from the API if more than 10 minutes have passed since the last update.
     *
     * @param balance the wallet balance (in FLOP)
     * @return the dollar value of the balance
     * @throws IOException if the API call fails
     */
    public double getValueInDollars(double balance) throws IOException {
        long now = System.currentTimeMillis();
        if (cachedRate == 0.0 || (now - lastRateUpdate) >= RATE_UPDATE_INTERVAL) {
            updateCoinRate();
            lastRateUpdate = now;
        }
        return cachedRate * balance;
    }

    /**
     * Fetches the current coin rate from the API and updates the cachedRate field.
     *
     * @throws IOException if there is an error during the API call.
     */
    private void updateCoinRate() throws IOException {
        String url = "https://api.livecoinwatch.com/coins/single";
        JSONObject payload = new JSONObject()
                .put("currency", "USD")
                .put("code", COIN_SYMBOL)
                .put("meta", true);

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("x-api-key", FlopBot.LIVECOINWATCH_API_KEY)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Error while trying to fetch coin price");
            }
            JSONObject json = new JSONObject(response.body().string());
            cachedRate = json.optDouble("rate", 0.0);
        }
    }
}
