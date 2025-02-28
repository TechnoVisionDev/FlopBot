package flopbot.data;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import flopbot.FlopBot;
import flopbot.data.cache.Stake;
import flopbot.data.cache.Wallet;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * Manages data between the bot and the MongoDB database.
 */
public class Database {

    /** Collections */
    public @NotNull MongoCollection<Wallet> wallets;
    public @NotNull MongoCollection<Stake> stakes;

    /**
     * Connect to database using MongoDB URI and initialize any collections that don't exist.
     * Additionally, check all stakes for spent/tampered UTXOs and remove them.
     *
     * @param uri MongoDB uri string.
     */
    public Database(String uri) {
        // Setup MongoDB database with URI.
        CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .codecRegistry(codecRegistry)
                .build();
        MongoClient mongoClient = MongoClients.create(clientSettings);
        MongoDatabase database = mongoClient.getDatabase("TechnoBot");

        // Initialize collections.
        wallets = database.getCollection("wallets", Wallet.class);
        stakes = database.getCollection("stakes", Stake.class);

        // Add custom indexing.
        wallets.createIndex(Indexes.descending("user"));
        stakes.createIndex(Indexes.ascending("txid"));

        // Iterate through all stakes and remove any with spent or tampered UTXOs.
        for (Stake stake : stakes.find()) {
            try {
                if (isUTXOSpent(stake.getTxid(), stake.getVout())) {
                    stakes.deleteOne(Filters.eq("_id", stake.getId()));
                }
            } catch (Exception ignored) { }
        }
    }

    /**
     * Checks if the UTXO for the given txid and vout is spent.
     *
     * @param txid The transaction ID.
     * @param vout The output index.
     * @return true if the UTXO is spent (or if an error occurs), false otherwise.
     */
    private boolean isUTXOSpent(String txid, int vout) {
        try {
            JSONObject result = sendRpcCommand("gettxout", txid, vout);
            return result == null;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Sends an RPC command using the provided method and parameters.
     *
     * @param method The RPC method.
     * @param params The RPC parameters.
     * @return A JSONObject representing the "result" field of the RPC response.
     * @throws Exception if an RPC error occurs.
     */
    private JSONObject sendRpcCommand(String method, Object... params) throws Exception {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("jsonrpc", "1.0");
        jsonRequest.put("id", "databaseCheckStakes");
        jsonRequest.put("method", method);
        JSONArray jsonParams = new JSONArray();
        for (Object param : params) {
            jsonParams.put(param);
        }
        jsonRequest.put("params", jsonParams);

        String auth = FlopBot.RPC_USER + ":" + FlopBot.RPC_PASSWORD;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(FlopBot.RPC_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + encodedAuth)
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject jsonResponse = new JSONObject(response.body());

        if (!jsonResponse.isNull("error") && !jsonResponse.get("error").toString().equals("null")) {
            JSONObject errorObj = jsonResponse.getJSONObject("error");
            throw new Exception(errorObj.optString("message", "Unknown RPC error"));
        }
        if (jsonResponse.isNull("result") || jsonResponse.get("result") == JSONObject.NULL) {
            return null;
        }
        return jsonResponse.getJSONObject("result");
    }
}
