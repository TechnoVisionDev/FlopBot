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
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class StakeClaimCommand extends Command {

    // 24 hours in milliseconds
    private static final long ONE_DAY_MS = 1000 * 60 * 60 * 24;
    private static final DecimalFormat amountFormatter = new DecimalFormat("#,##0.##");

    public StakeClaimCommand(FlopBot bot) {
        super(bot);
        this.name = "claim";
        this.description = "Claim daily rewards for an active stake.";
        this.category = Category.STAKING;
        this.args.add(new OptionData(OptionType.STRING, "txid", "The transaction ID of your active stake", true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        long userId = Long.parseLong(event.getUser().getId());
        String txid = event.getOption("txid").getAsString();

        // Retrieve the user's stake based on TXID.
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
        // Use the last claim time if available; otherwise, default to the stake's start time.
        long lastClaimTime = (stake.getLastClaim() > 0) ? stake.getLastClaim() : stake.getStartTime();

        if (now - lastClaimTime < ONE_DAY_MS) {
            long hoursRemaining = (ONE_DAY_MS - (now - lastClaimTime)) / (1000 * 60 * 60);
            MessageEmbed embed = new EmbedBuilder()
                    .setAuthor(event.getUser().getEffectiveName(), null, event.getUser().getEffectiveAvatarUrl())
                    .setDescription("You can only claim your stake reward once every 24 hours.\nApproximately `" + hoursRemaining + " hours` remaining until your next claim.")
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

        // Calculate the total staked amount across all stakes.
        List<Stake> stakes = new ArrayList<>();
        bot.database.stakes.find().into(stakes);
        double totalStaked = 0;
        for (Stake s : stakes) {
            totalStaked += s.getAmount();
        }
        if (totalStaked <= 0) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError("Total staked amount is zero: cannot calculate rewards.")).queue();
            return;
        }

        // Retrieve the faucet balance and calculate the daily reward pool.
        double faucetBalance = bot.getFaucetBalance();
        double monthlyRewardPool = faucetBalance * 0.1; // 10% of faucet balance distributed monthly.
        double dailyRewardPool = monthlyRewardPool / 30.0; // Distribute evenly over 30 days.

        // Calculate the user's share of the daily reward pool.
        double userStakeAmount = stake.getAmount();
        double userShare = userStakeAmount / totalStaked;
        long rewardAmount = (long) (dailyRewardPool * userShare);

        // Send the reward to the user.
        bot.walletHandler.sendCoins(userId, rewardAmount);

        // Update the stake's last claim time.
        stake.setLastClaim(now);
        bot.database.stakes.replaceOne(Filters.eq("_id", stake.getId()), stake);

        // Calculate percentage of total staked as a whole number,
        // but if less than 1%, display "< 1%".
        double percentageValue = userShare * 100.0;
        String stakePercentage;
        if (percentageValue < 1.0) {
            stakePercentage = "< 1%";
        } else {
            stakePercentage = String.format("%d%%", (int) percentageValue);
        }

        MessageEmbed successMessage = new EmbedBuilder()
                .setTitle("Daily Stake Reward Claimed!")
                .setColor(EmbedColor.SUCCESS.color)
                .setThumbnail("https://cdn-icons-png.flaticon.com/512/10384/10384161.png")
                .setDescription("Your daily stake reward has been credited to your account!\nUse `/balance` and `/withdraw` to manage your funds.")
                .addField("Staked Amount", amountFormatter.format(userStakeAmount) + " FLOP", false)
                .addField("Reward Amount", amountFormatter.format(rewardAmount) + " FLOP", false)
                .addField("Stake Percentage", stakePercentage, false)
                .addField("Stake TXID", "[View on Explorer](https://explorer.flopcoin.net/tx/" + stake.getTxid() + ")", false)
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
        jsonRequest.put("id", "stakeClaimCommand");
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
