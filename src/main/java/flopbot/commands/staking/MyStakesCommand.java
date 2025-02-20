package flopbot.commands.staking;

import com.mongodb.client.model.Filters;
import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.data.cache.Stake;
import flopbot.util.embeds.EmbedColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class MyStakesCommand extends Command {

    // Formatter for the stake amount.
    private static final DecimalFormat amountFormatter = new DecimalFormat("#,##0.##");

    // Formatter for the date.
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M/d/yyyy");

    public MyStakesCommand(FlopBot bot) {
        super(bot);
        this.name = "mystakes";
        this.description = "Display all your active stakes.";
        this.category = Category.LINKS;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        // Convert the Discord user ID to a long for querying the stakes.
        long userId = Long.parseLong(event.getUser().getId());

        // Query the stakes collection for all stakes belonging to this user.
        List<Stake> userStakes = new ArrayList<>();
        bot.database.stakes.find(Filters.eq("userId", userId)).into(userStakes);

        // Filter out any stakes whose UTXO has been spent.
        List<Stake> activeStakes = new ArrayList<>();
        for (Stake stake : userStakes) {
            if (isUTXOSpent(stake.getTxid(), stake.getVout())) {
                // UTXO is spent; remove it from the database.
                bot.database.stakes.deleteOne(Filters.eq("_id", stake.getId()));
            } else {
                activeStakes.add(stake);
            }
        }

        // If no active stakes remain, inform the user.
        if (activeStakes.isEmpty()) {
            event.getHook().sendMessage("You have no active stakes at this time.").queue();
            return;
        }

        // Create an embed to display the user's active stakes.
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(event.getUser().getEffectiveName() + "'s Stakes");
        eb.setColor(EmbedColor.DEFAULT.color);
        eb.setFooter("Total Stakes: " + activeStakes.size());
        eb.setThumbnail("https://cdn-icons-png.flaticon.com/512/9185/9185001.png");

        // Add each active stake's details as a field in the embed.
        for (Stake stake : activeStakes) {
            // Convert the startTime (epoch millis) to a formatted date.
            LocalDate date = Instant.ofEpochMilli(stake.getStartTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            String formattedDate = date.format(dateFormatter);

            // Format the amount.
            String formattedAmount = amountFormatter.format(stake.getAmount());
            String title = formattedAmount + " FLOP";
            String details = "**Date:** " + formattedDate + "\n"
                    + "**TXID:** [View on Explorer](https://explorer.flopcoin.net/tx/" + stake.getTxid() + ")";
            eb.addField(title, details, false);
        }

        // Send the embed as the reply.
        event.getHook().sendMessageEmbeds(eb.build()).queue();
    }

    /**
     * Checks if a UTXO is spent by calling the RPC "gettxout" method.
     *
     * @param txid the transaction ID.
     * @param vout the output index.
     * @return true if the UTXO is spent (i.e. RPC returns null), false otherwise.
     */
    private boolean isUTXOSpent(String txid, int vout) {
        try {
            JSONObject result = sendRpcCommand("gettxout", txid, vout);
            return result == null;  // A null result means the UTXO is spent or invalid.
        } catch (Exception e) {
            // If an error occurs, assume the UTXO is spent.
            return true;
        }
    }

    /**
     * Sends a JSON‑RPC request to the Flopcoin node and returns the result as a JSONObject.
     *
     * @param method The RPC method to call.
     * @param params The parameters for the RPC method.
     * @return The JSON object from the RPC result, or null if the result is null.
     * @throws Exception if the RPC call fails or the node returns an error.
     */
    private JSONObject sendRpcCommand(String method, Object... params) throws Exception {
        // Build JSON‑RPC request.
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("jsonrpc", "1.0");
        jsonRequest.put("id", "mystakesCommand");
        jsonRequest.put("method", method);
        JSONArray jsonParams = new JSONArray();
        for (Object param : params) {
            jsonParams.put(param);
        }
        jsonRequest.put("params", jsonParams);

        // Prepare Basic Auth header using credentials from FlopBot.
        String auth = FlopBot.RPC_USER + ":" + FlopBot.RPC_PASSWORD;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FlopBot.RPC_URL))
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
