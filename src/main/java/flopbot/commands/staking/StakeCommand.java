package flopbot.commands.staking;

import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import com.mongodb.client.model.Filters;
import flopbot.data.cache.Stake;
import flopbot.util.embeds.EmbedUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class StakeCommand extends Command {

    public StakeCommand(FlopBot bot) {
        super(bot);
        this.name = "stake";
        this.description = "Stake funds using a UTXO to earn rewards.";
        this.category = Category.STAKING;
        this.args.add(new OptionData(OptionType.STRING, "txid", "The transaction ID used for staking", true));
        this.args.add(new OptionData(OptionType.NUMBER, "amount", "The amount of FLOP to stake", true).setMinValue(1));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Defer reply to allow processing time.
        event.deferReply().queue();

        // Retrieve command options.
        String txid = event.getOption("txid").getAsString();
        double amount = event.getOption("amount").getAsDouble();
        long userId = Long.parseLong(event.getUser().getId());

        // Check if the stake has already been submitted.
        if (bot.database.stakes.find(Filters.eq("txid", txid)).first() != null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError("This stake has already been submitted!")).queue();
            return;
        }

        JSONObject utxoData;
        try {
            // Use RPC to get UTXO details for vout 0.
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

        // Identify the correct vout based on the provided amount by iterating possible vouts (0-9).
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

        // Extract the reward address from the UTXO data.
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

        // Insert the new stake document into MongoDB.
        try {
            bot.database.stakes.insertOne(newStake);
            event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess("Stake submitted successfully! Tracking confirmations.")).queue();
        } catch (Exception e) {
            e.printStackTrace();
            event.getHook().sendMessageEmbeds(EmbedUtils.createError("An error occurred while processing the stake.")).queue();
        }
    }

    /**
     * Sends a JSON‑RPC request to the Flopcoin node and returns the result as a JSONObject.
     *
     * @param method The RPC method to call.
     * @param params The parameters for the RPC method.
     * @return The JSON object from the RPC result.
     * @throws Exception if the RPC call fails or the node returns an error.
     */
    private JSONObject sendRpcCommand(String method, Object... params) throws Exception {
        // Build JSON‑RPC request.
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("jsonrpc", "1.0");
        jsonRequest.put("id", "stakeCommand");
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
        // Return the result object.
        return jsonResponse.getJSONObject("result");
    }
}
