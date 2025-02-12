package flopbot.commands.utility;

import flopbot.util.embeds.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.util.embeds.EmbedColor;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;

/**
 * Command that fetches crypto statistics using the CoinPaprika API.
 */
public class CoinStatsCommand extends Command {

    public CoinStatsCommand(FlopBot bot) {
        super(bot);
        this.name = "coinstats";
        this.description = "Display stats for any crypto by its ticker symbol.";
        this.category = Category.UTILITY;
        this.args.add(new OptionData(OptionType.STRING, "ticker", "Coin ticker symbol", true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Retrieve the "ticker" option from the slash command
        if (event.getOption("ticker") == null) {
            event.replyEmbeds(EmbedUtils.createError("You must provide a crypto ticker (e.g., BTC, ETH, FLOP, etc)."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        String ticker = event.getOption("ticker").getAsString().toUpperCase();

        try {
            // Configure the HttpClient to follow redirects automatically
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            // Build the search URL
            String searchUrl = "https://api.coinpaprika.com/v1/search?c=currencies&q=" + ticker;
            HttpRequest searchRequest = HttpRequest.newBuilder()
                    .uri(new URI(searchUrl))
                    .GET()
                    .build();
            HttpResponse<String> searchResponse = client.send(searchRequest, HttpResponse.BodyHandlers.ofString());

            // Check for a successful status code
            if (searchResponse.statusCode() != 200) {
                event.reply("Error: Received status code " + searchResponse.statusCode() + " from the API.")
                        .setEphemeral(true)
                        .queue();
                return;
            }
            String responseBody = searchResponse.body().trim();

            if (!responseBody.startsWith("{")) {
                event.reply("Unexpected API response format: " + responseBody)
                        .setEphemeral(true)
                        .queue();
                return;
            }

            JSONObject searchJson = new JSONObject(responseBody);
            JSONArray currencies = searchJson.getJSONArray("currencies");

            JSONObject coinSearchResult = null;
            for (int i = 0; i < currencies.length(); i++) {
                JSONObject coinObj = currencies.getJSONObject(i);
                if (coinObj.getString("symbol").equalsIgnoreCase(ticker)) {
                    coinSearchResult = coinObj;
                    break;
                }
            }

            if (coinSearchResult == null) {
                event.reply("Coin with ticker `" + ticker + "` not found.")
                        .setEphemeral(true)
                        .queue();
                return;
            }

            // Continue with fetching further details
            String coinId = coinSearchResult.getString("id");

            // 2. Get coin ticker info
            String tickerUrl = "https://api.coinpaprika.com/v1/tickers/" + coinId;
            HttpRequest tickerRequest = HttpRequest.newBuilder()
                    .uri(new URI(tickerUrl))
                    .GET()
                    .build();
            HttpResponse<String> tickerResponse = client.send(tickerRequest, HttpResponse.BodyHandlers.ofString());
            JSONObject tickerJson = new JSONObject(tickerResponse.body());

            // Extract required fields from the ticker endpoint
            String name = tickerJson.optString("name", "N/A");
            String symbol = tickerJson.optString("symbol", "N/A");

            JSONObject quotes = tickerJson.optJSONObject("quotes");
            JSONObject usdQuote = (quotes != null) ? quotes.optJSONObject("USD") : null;
            double price = (usdQuote != null) ? usdQuote.optDouble("price", 0.0) : 0.0;
            double volume24h = (usdQuote != null) ? usdQuote.optDouble("volume_24h", 0.0) : 0.0;
            double marketCap = (usdQuote != null) ? usdQuote.optDouble("market_cap", 0.0) : 0.0;

            double totalSupply = tickerJson.optDouble("total_supply", 0.0);
            double maxSupply = tickerJson.optDouble("max_supply", 0.0);

            // 3. Get additional coin info (e.g., website link, logo) from /coins/{coinId}
            String coinUrl = "https://api.coinpaprika.com/v1/coins/" + coinId;
            HttpRequest coinRequest = HttpRequest.newBuilder()
                    .uri(new URI(coinUrl))
                    .GET()
                    .build();
            HttpResponse<String> coinResponse = client.send(coinRequest, HttpResponse.BodyHandlers.ofString());
            JSONObject coinJson = new JSONObject(coinResponse.body());

            // Website link
            String website = "N/A";
            if (coinJson.has("links")) {
                JSONObject links = coinJson.getJSONObject("links");
                if (links.has("website")) {
                    JSONArray websiteArray = links.getJSONArray("website");
                    if (websiteArray.length() > 0) {
                        website = websiteArray.getString(0);
                    }
                }
            }

            // Coin logo
            String logoUrl = coinJson.optString("logo", "N/A");

            // 4. If market cap is N/A, calculate it using totalSupply * price
            if (marketCap == 0.0 && totalSupply != 0.0) {
                marketCap = totalSupply * price;
            }

            // 5. Format numbers

            // Format coin price:
            String formattedPrice;
            if (price < 1.0) {
                // For prices below $1: if greater than $0.01, show 4 decimals; otherwise, 8 decimals.
                if (price > 0.01) {
                    formattedPrice = String.format("%.4f", price);
                } else {
                    formattedPrice = String.format("%.8f", price);
                }
            } else if (price < 100) {
                // For prices between $1 and $100, use 2 decimals.
                NumberFormat priceFormatter = NumberFormat.getNumberInstance();
                priceFormatter.setMaximumFractionDigits(2);
                priceFormatter.setMinimumFractionDigits(2);
                formattedPrice = priceFormatter.format(price);
            } else if (price < 1_000_000) {
                // For prices between $100 and $1,000,000, show as an integer.
                formattedPrice = NumberFormat.getIntegerInstance().format(price);
            } else {
                // For prices above $1,000,000, use abbreviated formatting.
                formattedPrice = formatAbbreviated(price);
            }

            // Format volume and market cap:
            String formattedVolume = volume24h == 0.0 ? "N/A"
                    : (volume24h < 1_000_000 ? NumberFormat.getIntegerInstance().format(volume24h) : formatAbbreviated(volume24h));
            String formattedMarketCap = marketCap == 0.0 ? "N/A"
                    : (marketCap < 1_000_000 ? NumberFormat.getIntegerInstance().format(marketCap) : formatAbbreviated(marketCap));

            // Format total and max supply using our new helper:
            String formattedTotalSupply = totalSupply == 0.0 ? "N/A" : formatSupply(totalSupply);
            String formattedMaxSupply = maxSupply == 0.0 ? "Unlimited" : formatSupply(maxSupply);

            // 6. Build and send the embed with all the information
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(EmbedColor.DEFAULT.color)
                    .setTitle(name + " (" + symbol + ")")
                    .setThumbnail(logoUrl.equals("N/A") ? null : logoUrl)
                    .addField("Price", "$" + formattedPrice, true)
                    .addField("Volume", "$" + formattedVolume, true)
                    .addField("Total Supply", formattedTotalSupply, true)
                    .addField("Max Supply", formattedMaxSupply, true)
                    .addField("Market Cap", "$" + formattedMarketCap, true)
                    .addField("Website", website, true);

            event.replyEmbeds(embed.build()).queue();

        } catch (Exception e) {
            event.replyEmbeds(EmbedUtils.createError("An error occurred while fetching coin stats!"))
                    .setEphemeral(true)
                    .queue();
            e.printStackTrace();
        }
    }

    /**
     * Formats a number to an abbreviated string.
     * Supports abbreviations for trillions (T), billions (B), and millions (M).
     *
     * @param value the number to format
     * @return a formatted string
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

    /**
     * Formats supply numbers (total and max) in an abbreviated style.
     * If the number (after division) is whole, decimals are omitted.
     *
     * @param value the supply number to format
     * @return a formatted string
     */
    private static String formatSupply(double value) {
        if (value >= 1_000_000_000_000L) {
            double result = value / 1_000_000_000_000.0;
            if (result == Math.floor(result)) {
                return String.format("%.0fT", result);
            } else {
                return String.format("%.2fT", result);
            }
        } else if (value >= 1_000_000_000) {
            double result = value / 1_000_000_000.0;
            if (result == Math.floor(result)) {
                return String.format("%.0fB", result);
            } else {
                return String.format("%.2fB", result);
            }
        } else if (value >= 1_000_000) {
            double result = value / 1_000_000.0;
            if (result == Math.floor(result)) {
                return String.format("%.0fM", result);
            } else {
                return String.format("%.2fM", result);
            }
        } else {
            // For values less than 1,000, show as an integer if whole; otherwise, show up to 2 decimals.
            if (value == Math.floor(value)) {
                return NumberFormat.getIntegerInstance().format(value);
            } else {
                NumberFormat nf = NumberFormat.getNumberInstance();
                nf.setMaximumFractionDigits(2);
                return nf.format(value);
            }
        }
    }
}
