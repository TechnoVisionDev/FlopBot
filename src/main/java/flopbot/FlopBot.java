package flopbot;

import com.google.gson.Gson;
import flopbot.commands.CommandRegistry;
import flopbot.data.Database;
import flopbot.handlers.ButtonHandler;
import flopbot.handlers.WalletHandler;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Main class for FlopBot Discord Bot.
 * Initializes bot, database, and listeners.
 */
public class FlopBot {

    public Gson gson;
    public OkHttpClient httpClient;
    public final @NotNull Dotenv config;
    public final @NotNull JDA jda;
    public final @NotNull Database database;

    public final @NotNull WalletHandler walletHandler;

    // RPC connection details as static variables
    public static final String RPC_USER;
    public static final String RPC_PASSWORD;
    public static final String RPC_URL;
    public static final String LIVECOINWATCH_API_KEY;
    public static final String FAUCET_ADDRESS;
    public static final String COIN_EMOJI;

    // Static block to initialize the RPC variables from your .env or system environment
    static {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        RPC_USER = dotenv.get("RPC_USER", System.getenv("RPC_USER"));
        RPC_PASSWORD = dotenv.get("RPC_PASSWORD", System.getenv("RPC_PASSWORD"));
        RPC_URL = dotenv.get("RPC_URL", System.getenv("RPC_URL"));
        LIVECOINWATCH_API_KEY = dotenv.get("LIVECOINWATCH_API_KEY", System.getenv("LIVECOINWATCH_API_KEY"));
        FAUCET_ADDRESS = dotenv.get("FAUCET_ADDRESS", System.getenv("FAUCET_ADDRESS"));
        COIN_EMOJI = dotenv.get("COIN_EMOJI", System.getenv("COIN_EMOJI"));
    }

    public FlopBot() {
        // Setup HTTP tools
        gson = new Gson();
        httpClient = new OkHttpClient();

        // Setup Config
        config = Dotenv.configure().ignoreIfMissing().load();
        String token = config.get("TOKEN", System.getenv("TOKEN"));

        // Setup Database
        database = new Database(config.get("DATABASE", System.getenv("DATABASE")));

        // Set Handlers
        walletHandler = new WalletHandler(this);

        // Create Discord Bot
        jda = JDABuilder.createLight(token, EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT))
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.customStatus("/help | flopcoin.net"))
                .build();

        // Add Commands and Listeners
        jda.addEventListener(
                new CommandRegistry(this),
                new ButtonHandler()
        );
    }

    /**
     * Retrieves the faucet's balance using the getbalance RPC command.
     *
     * @return The current wallet balance.
     * @throws Exception if the RPC call fails.
     */
    public double getFaucetBalance() {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("jsonrpc", "1.0");
        jsonRequest.put("id", "getbalance");
        jsonRequest.put("method", "getbalance");
        // Some nodes require an empty array of parameters.
        JSONArray params = new JSONArray();
        jsonRequest.put("params", params);

        String auth = FlopBot.RPC_USER + ":" + FlopBot.RPC_PASSWORD;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FlopBot.RPC_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + encodedAuth)
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest.toString()))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject jsonResponse = new JSONObject(response.body());

            if (!jsonResponse.isNull("error") && !jsonResponse.get("error").toString().equals("null")) {
                return 0;
            }
            return jsonResponse.getDouble("result");
        } catch (InterruptedException | IOException e) {
            return 0;
        }
    }

    public static void main(String[] args) {
        FlopBot flopBot = new FlopBot();
    }
}
