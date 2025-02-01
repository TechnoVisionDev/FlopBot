package flopbot.handlers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mongodb.client.model.Filters;
import flopbot.FlopBot;
import flopbot.data.cache.Wallet;
import flopbot.data.json.RpcResponse;
import okhttp3.*;
import org.bson.conversions.Bson;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;

import static flopbot.FlopBot.*;
import static flopbot.handlers.StatsHandler.COIN_SYMBOL;

public class WalletHandler {

    public static final DecimalFormat FORMATTER = new DecimalFormat("#,###");

    private final OkHttpClient client;
    private final Gson gson;
    private final FlopBot bot;

    public WalletHandler(FlopBot bot) {
        this.bot = bot;
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    public String getAddress(long userID) throws IOException {
        Bson filter = Filters.eq("user", userID);
        Wallet wallet = bot.database.wallets.find(filter).first();
        if (wallet == null) {
            return createAddress(userID);
        }
        return wallet.getAddress();
    }

    public String createAddress(long userID) throws IOException {
        MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");
        // Build the JSON payload for the "getnewaddress" RPC call.
        String jsonPayload = "{\"jsonrpc\": \"1.0\", \"id\": \"java-newaddress\", \"method\": \"getnewaddress\", \"params\": []}";
        RequestBody body = RequestBody.create(jsonPayload, JSON_MEDIA);

        // Build the HTTP request with Basic Auth using your RPC credentials.
        Request request = new Request.Builder()
                .url(RPC_URL)
                .post(body)
                .header("Authorization", Credentials.basic(RPC_USER, RPC_PASSWORD))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected HTTP code " + response);
            }
            String responseBody = response.body().string();

            // Deserialize the JSON response. We expect the "result" to be a String.
            RpcResponse<String> rpcResponse = gson.fromJson(
                    responseBody,
                    new TypeToken<RpcResponse<String>>() {
                    }.getType()
            );

            if (rpcResponse.error != null) {
                throw new IOException("RPC Error: " + rpcResponse.error.message);
            }

            // Add to database
            Wallet wallet = new Wallet(userID, rpcResponse.result);
            bot.database.wallets.insertOne(wallet);

            return rpcResponse.result;
        }
    }

    /**
     * Retrieves the balance of the given address from the explorer API.
     *
     * @param address the wallet address to query.
     * @return the balance as a double.
     * @throws IOException if an I/O error occurs or the response is not successful.
     */
    public double getBalance(String address) throws IOException {
        // Build the API URL
        String url = "https://explorer.flopcoin.net/ext/getbalance/" + address;
        Request request = new Request.Builder().url(url).build();

        // Execute the request
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code());
            }
            String responseBody = response.body().string().trim();
            if (responseBody.startsWith("{") || responseBody.startsWith("[")) {
                return 0;
            }
            return Double.parseDouble(responseBody);
        }
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
