package flopbot.commands.staking;

import com.mongodb.client.model.Filters;
import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.data.cache.Stake;
import flopbot.util.embeds.EmbedColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

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

public class StakeListCommand extends Command {

    private static final DecimalFormat amountFormatter = new DecimalFormat("#,##0.##");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M/d/yyyy");

    public StakeListCommand(FlopBot bot) {
        super(bot);
        this.name = "list";
        this.description = "Display all your active stakes.";
        this.category = Category.STAKING;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        long userId = Long.parseLong(event.getUser().getId());
        List<Stake> userStakes = new ArrayList<>();
        bot.database.stakes.find(Filters.eq("userId", userId)).into(userStakes);

        List<Stake> activeStakes = new ArrayList<>();
        for (Stake stake : userStakes) {
            if (isUTXOSpent(stake.getTxid(), stake.getVout())) {
                bot.database.stakes.deleteOne(Filters.eq("_id", stake.getId()));
            } else {
                activeStakes.add(stake);
            }
        }

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

        for (Stake stake : activeStakes) {
            LocalDate date = Instant.ofEpochMilli(stake.getStartTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            String formattedDate = date.format(dateFormatter);
            String title = amountFormatter.format(stake.getAmount()) + " FLOP";
            String details = "**Date:** " + formattedDate + "\n"
                    + "**TXID:** [View on Explorer](https://explorer.flopcoin.net/tx/" + stake.getTxid() + ")\n"
                    + "**Vout:** " + stake.getVout();
            eb.addField(title, details, false);
        }
        event.getHook().sendMessageEmbeds(eb.build()).queue();
    }

    private boolean isUTXOSpent(String txid, int vout) {
        try {
            JSONObject result = sendRpcCommand("gettxout", txid, vout);
            return result == null;
        } catch (Exception e) {
            return true;
        }
    }

    private JSONObject sendRpcCommand(String method, Object... params) throws Exception {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("jsonrpc", "1.0");
        jsonRequest.put("id", "stakeListCommand");
        jsonRequest.put("method", method);
        JSONArray jsonParams = new JSONArray();
        for (Object param : params) {
            jsonParams.put(param);
        }
        jsonRequest.put("params", jsonParams);
        String auth = FlopBot.RPC_USER + ":" + FlopBot.RPC_PASSWORD;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(FlopBot.RPC_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + encodedAuth)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonRequest.toString()))
                .build();
        java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
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
