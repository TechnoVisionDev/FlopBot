package flopbot;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Main class for FlopBot Discord Bot.
 * Initializes bot, database, and listeners.
 *
 * @author TechnoVision
 */
public class FlopBot {

    public final @NotNull Dotenv config;

    public FlopBot() {
        config = Dotenv.configure().ignoreIfMissing().load();
        String token = config.get("TOKEN", System.getenv("TOKEN"));

        JDABuilder.createLight(token, EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT))
                .addEventListeners()
                .build();
    }

    public static void main(String[] args) {
        FlopBot flopBot = new FlopBot();
    }
}
