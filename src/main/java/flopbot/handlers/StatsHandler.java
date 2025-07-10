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
 * A simplified stats handler that updates four channels:
 * - One showing the total Discord member count.
 * - One displaying the current FLOP coin price from CoinPaprika.
 * - One displaying the 24 h Volume.
 * - One displaying the Market Cap.
 */
public class StatsHandler extends ListenerAdapter {

    private static final Logger LOGGER = Logger.getLogger(StatsHandler.class.getName());

    // Channel name prefixes
    private static final String DISCORD_CHANNEL_PREFIX   = "\uD83D\uDE80 MEMBERS: ";
    private static final String FLOP_PRICE_CHANNEL_PREFIX = "\uD83D\uDCB2FLOP: $";
    private static final String FLOP_VOL_CHANNEL_PREFIX   = "\uD83D\uDCB2VOL: $";
    private static final String FLOP_MC_CHANNEL_PREFIX    = "\uD83D\uDCB2MC: $";

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.########");
    public static final DecimalFormat SUFFIX_FORMAT = new DecimalFormat("0.###");

    // CoinPaprika coin ID for FLOP.
    public static final String COIN_PAPRIKA_ID = "flop-flopcoin";
    public static final String SUPPLY_URL      = "https://explorer.flopcoin.net/ext/getmoneysupply";

    // Required configuration from environment variables or .env file
    private final @NotNull String GUILD_ID;

    public StatsHandler(Dotenv config) {
        GUILD_ID = config.get("GUILD_ID", System.getenv("GUILD_ID"));
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        // Schedule updates every hour (3_600_000 ms)
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
            if (guild == null) return;

            try {
                // Fetch the USD quote object once
                JSONObject usdQuote = fetchUsdQuote();

                double price     = usdQuote.getDouble("price");
                double volume24h = usdQuote.getDouble("volume_24h");

                // Get circulating supply from your explorer
                double supply    = fetchMoneySupply();

                // Compute market cap
                double marketCap = price * supply;

                String priceStr = DECIMAL_FORMAT.format(price);
                String volStr   = formatWithSuffix(volume24h);
                String mcStr    = formatWithSuffix(marketCap);

                int memberCount = guild.getMemberCount();

                // Update (or create) each channel
                updateChannel(guild, DISCORD_CHANNEL_PREFIX,   DISCORD_CHANNEL_PREFIX   + memberCount);
                updateChannel(guild, FLOP_PRICE_CHANNEL_PREFIX, FLOP_PRICE_CHANNEL_PREFIX + priceStr);
                updateChannel(guild, FLOP_VOL_CHANNEL_PREFIX,   FLOP_VOL_CHANNEL_PREFIX   + volStr);
                updateChannel(guild, FLOP_MC_CHANNEL_PREFIX,    FLOP_MC_CHANNEL_PREFIX    + mcStr);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error updating channels", e);
            }
        }

        private double fetchMoneySupply() throws IOException {
            Request req = new Request.Builder()
                    .url(SUPPLY_URL)
                    .header("User-Agent", "FlopBot/1.0")
                    .build();

            try (Response resp = httpClient.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    LOGGER.warning("Explorer API error: " + resp);
                    throw new IOException("Unexpected explorer response " + resp);
                }
                // The endpoint returns the supply as a plain number string
                String body = resp.body().string().trim();
                return Double.parseDouble(body);
            }
        }

        /**
         * Fetches the USD quote object from CoinPaprika.
         */
        private JSONObject fetchUsdQuote() throws IOException {
            String url = "https://api.coinpaprika.com/v1/tickers/" + COIN_PAPRIKA_ID;
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "FlopBot/1.0")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOGGER.warning("CoinPaprika API error: " + response);
                    throw new IOException("Unexpected response " + response);
                }
                JSONObject json = new JSONObject(response.body().string());
                return json.getJSONObject("quotes").getJSONObject("USD");
            }
        }

        /**
         * Updates an existing voice channel (matching the prefix) or creates it if it doesn't exist.
         */
        private void updateChannel(Guild guild, String prefix, String newName) {
            for (VoiceChannel channel : guild.getVoiceChannels()) {
                if (channel.getName().startsWith(prefix)) {
                    if (!channel.getName().equals(newName)) {
                        channel.getManager().setName(newName).queue();
                    }
                    return;
                }
            }
            guild.createVoiceChannel(newName).queue();
        }

        /**
         * Formats a large number with k/M/B suffix, preserving up to three
         * decimal places. Examples:
         *   39128   → "39.128k"
         *   130000  → "130k"
         *   5700000 → "5.7M"
         */
        private String formatWithSuffix(double value) {
            if (value >= 1_000_000_000) {
                return SUFFIX_FORMAT.format(value / 1_000_000_000) + "B";
            } else if (value >= 1_000_000) {
                return SUFFIX_FORMAT.format(value / 1_000_000) + "M";
            } else if (value >= 1_000) {
                return SUFFIX_FORMAT.format(value / 1_000) + "k";
            } else {
                return SUFFIX_FORMAT.format(value);
            }
        }
    }
}
