package flopbot;

import com.google.gson.Gson;
import flopbot.commands.CommandRegistry;
import flopbot.data.Database;
import flopbot.handlers.StatsHandler;
import flopbot.handlers.WalletHandler;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Main class for FlopBot Discord Bot.
 * Initializes bot, database, and listeners.
 */
public class FlopBot {

    public static final String flopcoinEmoji = "<:flopcoin:1335028565128777759>";
    public static final String fLogoEmoji = "<:f_logo:1335028928879919116>";

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

    // Static block to initialize the RPC variables from your .env or system environment
    static {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        RPC_USER = dotenv.get("RPC_USER", System.getenv("RPC_USER"));
        RPC_PASSWORD = dotenv.get("RPC_PASSWORD", System.getenv("RPC_PASSWORD"));
        RPC_URL = dotenv.get("RPC_URL", System.getenv("RPC_URL"));
        LIVECOINWATCH_API_KEY = dotenv.get("LIVECOINWATCH_API_KEY", System.getenv("LIVECOINWATCH_API_KEY"));
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
                new StatsHandler(config)
        );
    }

    public static void main(String[] args) {
        FlopBot flopBot = new FlopBot();
    }
}
