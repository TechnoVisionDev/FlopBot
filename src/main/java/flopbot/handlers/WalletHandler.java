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

    public double getValueInDollars(double balance) throws IOException {
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
            return new JSONObject(response.body().string()).optDouble("rate", 0.0) * balance;
        }
    }
}
