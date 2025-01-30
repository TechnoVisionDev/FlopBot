package flopbot;

import com.google.gson.Gson;
import flopbot.commands.CommandRegistry;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Main class for FlopBot Discord Bot.
 * Initializes bot, database, and listeners.
 *
 * @author TechnoVision
 */
public class FlopBot {

    public Gson gson;
    public OkHttpClient httpClient;

    public final @NotNull Dotenv config;
    public final @NotNull JDA jda;

    public FlopBot() {
        //Setup HTTP tools
        gson = new Gson();
        httpClient = new OkHttpClient();

        //Setup Config
        config = Dotenv.configure().ignoreIfMissing().load();
        String token = config.get("TOKEN", System.getenv("TOKEN"));

        jda = JDABuilder.createLight(token, EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT))
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.playing("/help | flopcoin.net"))
                .build();

        jda.addEventListener(new CommandRegistry(this));
    }

    public static void main(String[] args) {
        FlopBot flopBot = new FlopBot();
    }
}
