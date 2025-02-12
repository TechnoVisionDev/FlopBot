package flopbot.handlers;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simplified stats handler that updates two channels:
 * - One showing the total Discord member count.
 * - One displaying the current FLOP coin price from CoinPaprika.
 */
public class StatsHandler extends ListenerAdapter {

    private static final Logger LOGGER = Logger.getLogger(StatsHandler.class.getName());

    // Channel name prefixes
    private static final String DISCORD_CHANNEL_PREFIX = "\uD83D\uDE80 MEMBERS: ";
    private static final String FLOP_PRICE_CHANNEL_PREFIX = "\uD83D\uDCB2FLOP: $";

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.########");

    // CoinPaprika coin ID for FLOP.
    // Change this value if your coin uses a different ID on CoinPaprika.
    public static final String COIN_PAPRIKA_ID = "flop-flopcoin";

    // Required configuration from environment variables or .env file
    private final @NotNull String GUILD_ID;

    public StatsHandler(Dotenv config) {
        GUILD_ID = config.get("GUILD_ID", System.getenv("GUILD_ID"));
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        // Schedule updates every 5 minutes (300,000 milliseconds)
        new Timer().schedule(new UpdateTask(event.getJDA(), this), 0, 3_600_000);
    }

    private static class UpdateTask extends TimerTask {
        private final JDA jda;
        private final StatsHandler statsHandler;
        private final OkHttpClient httpClient = new OkHttpClient();

        public UpdateTask(JDA jda, StatsHandler statsHandler) {
            this.jda = jda;
            this.statsHandler = statsHandler;
        }

        @Override
        public void run() {
            Guild guild = jda.getGuildById(statsHandler.GUILD_ID);
            if (guild == null) { return; }

            try {
                // Fetch FLOP coin price from CoinPaprika API
                double rate = fetchCoinPrice();
                String price = DECIMAL_FORMAT.format(rate);

                // Get total Discord member count
                int discordMemberCount = guild.getMemberCount();

                // Update channels with the latest stats
                updateChannel(guild, DISCORD_CHANNEL_PREFIX, DISCORD_CHANNEL_PREFIX + discordMemberCount);
                updateChannel(guild, FLOP_PRICE_CHANNEL_PREFIX, FLOP_PRICE_CHANNEL_PREFIX + price);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error updating channels", e);
            }
        }

        /**
         * Updates an existing voice channel (matching the prefix) or creates it if it doesn't exist.
         *
         * @param guild   the guild where the channel is located
         * @param prefix  the channel name prefix to look for
         * @param newName the new channel name
         */
        private void updateChannel(Guild guild, String prefix, String newName) {
            for (VoiceChannel channel : guild.getVoiceChannels()) {
                if (channel.getName().startsWith(prefix)) {
                    if (!channel.getName().equals(newName)) { // Update only if the name has changed
                        channel.getManager().setName(newName).queue();
                    }
                    return;
                }
            }
            // No matching channel found; create a new one.
            guild.createVoiceChannel(newName).queue();
        }

        /**
         * Fetches the current coin price from the CoinPaprika API.
         *
         * @return the coin price in USD, or 0.0 if an error occurs
         * @throws IOException if an I/O error occurs during the API call
         */
        private double fetchCoinPrice() throws IOException {
            String url = "https://api.coinpaprika.com/v1/tickers/" + COIN_PAPRIKA_ID;
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "FlopBot/1.0")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOGGER.warning("CoinPaprika API error: " + response);
                    return 0.0;
                }
                JSONObject json = new JSONObject(response.body().string());
                // Extract the USD price from the "quotes" object
                return json.getJSONObject("quotes").getJSONObject("USD").getDouble("price");
            }
        }
    }
}
