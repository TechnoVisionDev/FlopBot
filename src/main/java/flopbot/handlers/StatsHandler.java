package flopbot.handlers;

import flopbot.FlopBot;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.user.UserV2;
import io.github.redouane59.twitter.signature.TwitterCredentials;
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
 * Handles live updating of coin stats for Discord server.
 * API Keys can be sent in the .env file.
 *
 * @author TechnoVision
 */
public class StatsHandler extends ListenerAdapter {

    private static final Logger LOGGER = Logger.getLogger(StatsHandler.class.getName());

    // Channel name prefixes
    private static final String DISCORD_CHANNEL_PREFIX = "Discord: ";
    private static final String REDDIT_CHANNEL_PREFIX = "Reddit: ";
    private static final String TWITTER_CHANNEL_PREFIX = "Twitter: ";
    private static final String FLOP_PRICE_CHANNEL_PREFIX = "\uD83D\uDCB2FLOP: ";
    private static final String FLOP_VOLUME_CHANNEL_PREFIX = "\uD83D\uDCB2VOL: ";

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.########");

    public static final String COIN_SYMBOL = "FLOP";
    public static final String REDDIT_SUB = "flopcoin";
    public static final String X_HANDLE = "flopcoin_dev";

    // API Keys and configuration
    public final @NotNull String GUILD_ID;
    public final @NotNull String X_API_KEY;
    public final @NotNull String X_API_SECRET;
    public final @NotNull String X_ACCESS_TOKEN;
    public final @NotNull String X_ACCESS_SECRET;
    public final @NotNull String X_BEARER_TOKEN;

    public StatsHandler(Dotenv config) {
        GUILD_ID = config.get("GUILD_ID", System.getenv("GUILD_ID"));
        X_API_KEY = config.get("X_API_KEY", System.getenv("X_API_KEY"));
        X_API_SECRET = config.get("X_API_SECRET", System.getenv("X_API_SECRET"));
        X_ACCESS_TOKEN = config.get("X_ACCESS_TOKEN", System.getenv("X_ACCESS_TOKEN"));
        X_ACCESS_SECRET = config.get("X_ACCESS_SECRET", System.getenv("X_ACCESS_SECRET"));
        X_BEARER_TOKEN = config.get("X_BEARER_TOKEN", System.getenv("X_BEARER_TOKEN"));
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        // Schedule channel updates every 5 minutes (300_000 milliseconds)
        new Timer().schedule(new UpdateTask(event.getJDA(), this), 0, 300_000);
    }

    private static class UpdateTask extends TimerTask {
        private final JDA jda;
        private final StatsHandler statsHandler;
        private final OkHttpClient httpClient = new OkHttpClient();
        private final TwitterClient twitterClient;

        public UpdateTask(JDA jda, StatsHandler statsHandler) {
            this.jda = jda;
            this.statsHandler = statsHandler;
            // Initialize the Twitter client once per update task
            twitterClient = new TwitterClient(new TwitterCredentials(
                    statsHandler.X_API_KEY,
                    statsHandler.X_API_SECRET,
                    statsHandler.X_ACCESS_TOKEN,
                    statsHandler.X_ACCESS_SECRET,
                    statsHandler.X_BEARER_TOKEN));
        }

        @Override
        public void run() {
            Guild guild = jda.getGuildById(statsHandler.GUILD_ID);
            if (guild == null) {
                LOGGER.warning("Guild not found for ID: " + statsHandler.GUILD_ID);
                return;
            }

            try {
                // Fetch stats from external APIs
                JSONObject coinStats = fetchCoinStats();
                double rate = coinStats.optDouble("rate", 0.0);
                double volumeValue = coinStats.optDouble("volume", 0.0);
                String price = "$" + DECIMAL_FORMAT.format(rate);
                String volume = formatVolume(volumeValue);

                int discordMemberCount = guild.getMemberCount();
                int redditSubscribers = fetchRedditSubscribers();
                int twitterFollowers = fetchTwitterFollowers();

                // Update channels with the latest stats
                updateChannel(guild, DISCORD_CHANNEL_PREFIX, DISCORD_CHANNEL_PREFIX + discordMemberCount);
                updateChannel(guild, REDDIT_CHANNEL_PREFIX, REDDIT_CHANNEL_PREFIX + redditSubscribers);
                updateChannel(guild, FLOP_PRICE_CHANNEL_PREFIX, FLOP_PRICE_CHANNEL_PREFIX + price);
                updateChannel(guild, FLOP_VOLUME_CHANNEL_PREFIX, FLOP_VOLUME_CHANNEL_PREFIX + volume);
                updateChannel(guild, TWITTER_CHANNEL_PREFIX, TWITTER_CHANNEL_PREFIX + twitterFollowers);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error updating stats channels", e);
            }
        }

        /**
         * Formats a volume value into a human-readable string (e.g., 1.0M, 2.5B).
         */
        private String formatVolume(double volume) {
            if (volume >= 1_000_000_000) {
                return String.format("%.1fB", volume / 1_000_000_000);
            } else if (volume >= 1_000_000) {
                return String.format("%.1fM", volume / 1_000_000);
            } else if (volume >= 1_000) {
                return String.format("%.0fk", volume / 1_000);
            } else {
                return String.valueOf((int) volume);
            }
        }

        /**
         * Finds an existing voice channel with the given prefix and updates its name,
         * or creates one if it doesn't exist.
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
         * Fetches the number of subscribers from the specified Reddit subreddit.
         */
        private int fetchRedditSubscribers() throws IOException {
            String url = "https://www.reddit.com/r/" + REDDIT_SUB + "/about.json";
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOGGER.warning("Failed to fetch Reddit subscribers: " + response);
                    return 0;
                }
                JSONObject json = new JSONObject(response.body().string());
                return json.getJSONObject("data").getInt("subscribers");
            }
        }

        /**
         * Fetches the Twitter follower count for the given handle.
         */
        private int fetchTwitterFollowers() {
            try {
                UserV2 user = twitterClient.getUserFromUserName(X_HANDLE);
                if (user != null && user.getData() != null && user.getData().getPublicMetrics() != null) {
                    return user.getData().getPublicMetrics().getFollowersCount();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error fetching Twitter followers", e);
            }
            return 0;
        }

        /**
         * Fetches coin statistics from the LiveCoinWatch API.
         */
        private JSONObject fetchCoinStats() throws IOException {
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

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOGGER.warning("LiveCoinWatch API error: " + response);
                    return new JSONObject();
                }
                return new JSONObject(response.body().string());
            }
        }
    }
}