package flopbot.commands.staking;

import com.mongodb.client.model.Filters;
import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.data.cache.Stake;
import flopbot.util.embeds.EmbedUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class StakeNewCommand extends Command {

    public StakeNewCommand(FlopBot bot) {
        super(bot);
        this.name = "new"; // subcommand name
        this.description = "Create a new stake using a UTXO.";
        this.category = Category.STAKING;
        // Options are defined in the parent command.
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        String txid = event.getOption("txid").getAsString();
        double amount = event.getOption("amount").getAsDouble();
        long userId = Long.parseLong(event.getUser().getId());

        // Check if the stake already exists.
        if (bot.database.stakes.find(Filters.eq("txid", txid)).first() != null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError("This stake has already been submitted!")).queue();
            return;
        }

        JSONObject utxoData;
        try {
            // Get UTXO details from the RPC for vout 0.
            utxoData = sendRpcCommand("gettxout", txid, 0);
        } catch (Exception e) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError("Invalid TXID or UTXO is already spent.")).queue();
            return;
        }

        if (utxoData == null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError("Invalid UTXO or already spent.")).queue();
            return;
        }

        int confirmations = utxoData.optInt("confirmations", 0);
        if (confirmations > 10) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError("UTXO has more than 10 confirmations. Submission denied.")).queue();
            return;
        }

        // Identify the correct vout by checking vout indices 0 to 9.
        Integer vout = null;
        for (int i = 0; i < 10; i++) {
            JSONObject outputData;
            try {
                outputData = sendRpcCommand("gettxout", txid, i);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            if (outputData != null && Double.compare(outputData.optDouble("value", 0.0), amount) == 0) {
                vout = i;
                break;
            }
        }
        if (vout == null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError("No matching UTXO found for the given amount.")).queue();
            return;
        }

        // Extract the reward address from the UTXO.
        JSONObject scriptPubKey = utxoData.optJSONObject("scriptPubKey");
        if (scriptPubKey == null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError("No valid reward address found for this UTXO.")).queue();
            return;
        }
        JSONArray addresses = scriptPubKey.optJSONArray("addresses");
        if (addresses == null || addresses.isEmpty()) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError("No valid reward address found for this UTXO.")).queue();
            return;
        }
        String rewardWallet = addresses.getString(0);

        // Create a new Stake instance.
        Stake newStake = new Stake(userId, txid, vout, rewardWallet, amount);
        // (Ensure your Stake constructor sets the startTime to System.currentTimeMillis())

        try {
            bot.database.stakes.insertOne(newStake);
            event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess("Stake submitted successfully! Tracking confirmations.")).queue();
        } catch (Exception e) {
            e.printStackTrace();
            event.getHook().sendMessageEmbeds(EmbedUtils.createError("An error occurred while processing the stake.")).queue();
        }
    }

    private JSONObject sendRpcCommand(String method, Object... params) throws Exception {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("jsonrpc", "1.0");
        jsonRequest.put("id", "stakeNewCommand");
        jsonRequest.put("method", method);
        JSONArray jsonParams = new JSONArray();
        for (Object param : params) {
            jsonParams.put(param);
        }
        jsonRequest.put("params", jsonParams);
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
        return jsonResponse.getJSONObject("result");
    }
}
