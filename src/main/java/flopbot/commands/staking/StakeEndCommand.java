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

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

public class StakeEndCommand extends Command {

    // 30 days in milliseconds (approximation of one month)
    private static final long ONE_DAY_MS = 1000 * 60 * 60 * 24;
    private static final long ONE_MONTH_MS = 30L * ONE_DAY_MS;
    private static final DecimalFormat amountFormatter = new DecimalFormat("#,##0.##");

    public StakeEndCommand(FlopBot bot) {
        super(bot);
        this.name = "end";
        this.description = "End an active stake (specify TXID) after 1 month to receive a 2% reward.";
        this.category = Category.STAKING;
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

        long now = System.currentTimeMillis();
        long elapsedMs = now - stake.getStartTime();

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

        // Check if the UTXO is still unspent.
        if (isUTXOSpent(txid, stake.getVout())) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError("This stake's UTXO has been spent or tampered with, so no rewards can be granted.")).queue();
            bot.database.stakes.deleteOne(Filters.eq("_id", stake.getId()));
            return;
        }

        long rewardAmount = (long) (stake.getAmount() * 0.02);
        bot.walletHandler.sendCoins(userId, rewardAmount);
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
        jsonRequest.put("id", "stakeEndCommand");
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
