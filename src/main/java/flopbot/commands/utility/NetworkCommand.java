package flopbot.commands.utility;

import flopbot.util.embeds.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.util.embeds.EmbedColor;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;

/**
 * Command that fetches network statistics for Flopcoin using the block explorer API.
 *
 * @author TechnoVision
 */
public class NetworkCommand extends Command {

    public NetworkCommand(FlopBot bot) {
        super(bot);
        this.name = "network";
        this.description = "Display stats for the Flopcoin network.";
        this.category = Category.UTILITY;
        // No additional options needed
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        try {
            // Create an HTTP client
            HttpClient client = HttpClient.newHttpClient();

            // Build the request to the Flopcoin block explorer API
            String url = "https://explorer.flopcoin.net/ext/getsummary";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                event.replyEmbeds(EmbedUtils.createError("Error: Received status code "
                                + response.statusCode() + " from the API."))
                        .setEphemeral(true)
                        .queue();
                return;
            }

            // Parse the response JSON
            String body = response.body().trim();
            JSONObject json = new JSONObject(body);

            // Extract the fields from the API response
            double difficulty = json.optDouble("difficulty", 0.0);
            // The "hashrate" is provided as a string, so parse it as a double.
            double hashrate = 0.0;
            try {
                hashrate = Double.parseDouble(json.getString("hashrate"));
            } catch (Exception ex) {
                // If parsing fails, hashrate remains 0.0
            }
            int blockHeight = json.optInt("blockcount", 0);

            // Calculate Block Reward:
            // Initial block reward: 500,000 FLOP, halving every 100,000 blocks until a floor of 10,000 FLOP.
            int halvingPeriods = blockHeight / 100000;
            double blockReward = 500000.0 / Math.pow(2, halvingPeriods);
            if (blockReward < 10000) {
                blockReward = 10000;
            }

            // Extract additional fields: connections and total supply.
            int connections = json.optInt("connections", 0);
            double supply = json.optDouble("supply", 0.0);

            // Format numbers

            // Format Difficulty: 3 decimals, comma-separated (e.g., "11,231.311")
            NumberFormat decimalFormatter = NumberFormat.getNumberInstance();
            decimalFormatter.setMinimumFractionDigits(3);
            decimalFormatter.setMaximumFractionDigits(3);
            String formattedDifficulty = decimalFormatter.format(difficulty);

            // Format Hashrate: 3 decimals and append " GH/s"
            String formattedHashrate = decimalFormatter.format(hashrate) + " GH/s";

            // Format Block Height as an integer with commas (e.g., "59,574")
            String formattedBlockHeight = NumberFormat.getIntegerInstance().format(blockHeight);

            // Format Block Reward as an integer with commas (e.g., "500,000")
            String formattedBlockReward = NumberFormat.getIntegerInstance().format((int) Math.round(blockReward));

            // Format Connections as an integer (e.g., "42")
            String formattedConnections = NumberFormat.getIntegerInstance().format(connections);

            // Format Total Supply using abbreviated notation (e.g., "21.31B")
            String formattedSupply = formatAbbreviated(supply);

            // Build the embed with 2 fields per row (using a filler field with inline=false for a line break)
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(EmbedColor.DEFAULT.color)
                    .setTitle("Flopcoin Network")
                    .setThumbnail("https://i.imgur.com/Y2ttbtX.png")
                    .addField("Difficulty", formattedDifficulty, true)
                    .addField("Hashrate", formattedHashrate, true)
                    .addField("Block Height", formattedBlockHeight, true)
                    .addField("Block Reward", formattedBlockReward, true)
                    .addField("Peers", formattedConnections, true)
                    .addField("Coin Supply", formattedSupply, true);

            event.replyEmbeds(embed.build()).queue();

        } catch (Exception e) {
            event.replyEmbeds(EmbedUtils.createError("An error occurred while fetching mining stats!"))
                    .setEphemeral(true)
                    .queue();
            e.printStackTrace();
        }
    }

    /**
     * Formats a number to an abbreviated string.
     * For example, 21310000000 -> "21.31B"
     *
     * @param value the number to format
     * @return the abbreviated string
     */
    private static String formatAbbreviated(double value) {
        if (value >= 1_000_000_000_000L) {
            return String.format("%.2fT", value / 1_000_000_000_000.0);
        } else if (value >= 1_000_000_000) {
            return String.format("%.2fB", value / 1_000_000_000.0);
        } else if (value >= 1_000_000) {
            return String.format("%.2fM", value / 1_000_000.0);
        } else {
            return NumberFormat.getNumberInstance().format(value);
        }
    }
}
