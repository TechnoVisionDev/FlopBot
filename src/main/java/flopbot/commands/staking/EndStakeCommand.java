package flopbot.commands.staking;

import com.mongodb.client.model.Filters;
import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.data.cache.Stake;
import flopbot.util.embeds.EmbedColor;
import flopbot.util.embeds.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.text.DecimalFormat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EndStakeCommand extends Command {

    // 30 days in milliseconds (approximation of one month)
    private static final long ONE_DAY_MS = 1000 * 60 * 60 * 24;
    private static final long ONE_MONTH_MS = 30L * ONE_DAY_MS;
    private static final DecimalFormat amountFormatter = new DecimalFormat("#,##0.##");

    public EndStakeCommand(FlopBot bot) {
        super(bot);
        this.name = "endstake";
        this.description = "End your stake after 1 month to receive a 2% reward.";
        this.category = Category.STAKING;
        // Require a TXID as an option.
        this.args.add(new OptionData(OptionType.STRING, "txid", "The transaction ID of an active stake", true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        long userId = Long.parseLong(event.getUser().getId());
        String txid = event.getOption("txid").getAsString();

        // Find the stake with the specified TXID that belongs to the user.
        Stake stake = bot.database.stakes.find(
                Filters.and(
                        Filters.eq("userId", userId),
                        Filters.eq("txid", txid)
                )
        ).first();

        if (stake == null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError("No active stake found with the provided TXID.")).queue();
            return;
        }

        // Calculate how long the stake has been active.
        long now = System.currentTimeMillis();
        long elapsedMs = now - stake.getStartTime();

        // If the stake hasn't been active for a full month, do not cancel it.
        if (elapsedMs < ONE_MONTH_MS) {
            long daysRemaining = (ONE_MONTH_MS - elapsedMs) / ONE_DAY_MS;
            MessageEmbed embed = new EmbedBuilder()
                    .setAuthor(event.getUser().getEffectiveName(), null, event.getUser().getEffectiveAvatarUrl())
                    .setDescription("You must stake for 1 month before earning rewards!\nApproximately `" + daysRemaining + " days` left for this stake.")
                    .setColor(EmbedColor.ERROR.color)
                    .setThumbnail("https://cdn0.iconfinder.com/data/icons/small-n-flat/24/678129-lock-512.png")
                    .build();
            event.getHook().sendMessageEmbeds(embed).queue();
            return;
        }

        // Before awarding the reward, verify that the UTXO is still unspent.
        if (isUTXOSpent(txid, stake.getVout())) {
            // UTXO has been spent or tampered with.
            event.getHook().sendMessageEmbeds(EmbedUtils.createError("This stake's UTXO has been spent or tampered with, so no rewards can be granted.")).queue();
            // If so, remove the stake from the database.
            bot.database.stakes.deleteOne(Filters.eq("_id", stake.getId()));
            return;
        }

        // Calculate stake reward of 2%.
        long rewardAmount = (long) (stake.getAmount() * 0.02);

        // Add reward to user's balance.
        bot.walletHandler.sendCoins(userId, rewardAmount);

        // Remove the stake from the database.
        bot.database.stakes.deleteOne(Filters.eq("_id", stake.getId()));

        MessageEmbed successMessage = new EmbedBuilder()
                .setTitle("Your Stake Has Completed!")
                .setColor(EmbedColor.SUCCESS.color)
                .setThumbnail("https://cdn-icons-png.flaticon.com/512/10384/10384161.png")
                .setDescription("Your reward has been sent to your account balance!\nUse `/balance` and `/withdraw` to access these funds.")
                .addField("Initial Stake", amountFormatter.format(stake.getAmount()) + " FLOP", false)
                .addField("Reward Amount (2%)", amountFormatter.format(rewardAmount) + " FLOP", false)
                .addField("Transaction ID", "[View on Explorer](https://explorer.flopcoin.net/tx/" + stake.getTxid() + ")", false)
                .build();
        event.getHook().sendMessageEmbeds(successMessage).queue();
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
        jsonRequest.put("id", "endstakeCommand");
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
