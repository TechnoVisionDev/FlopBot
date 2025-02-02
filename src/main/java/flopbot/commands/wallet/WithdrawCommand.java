package flopbot.commands.wallet;

import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.util.embed.EmbedColor;
import flopbot.util.embed.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import static net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Base64;
import org.json.JSONObject;
import org.json.JSONArray;

public class WithdrawCommand extends Command {

    private static final DecimalFormat FORMATTER = new DecimalFormat("#,###");

    public WithdrawCommand(FlopBot bot) {
        super(bot);
        this.name = "withdraw";
        this.description = "Withdraw funds to an external wallet.";
        this.category = Category.WALLET;
        this.args.add(new OptionData(INTEGER, "amount", "The amount of FLOP to withdraw", true)
                .setMinValue(1)
                .setMaxValue(Integer.MAX_VALUE));
        this.args.add(new OptionData(STRING, "address", "The wallet address to withdraw funds to", true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Defer the reply since the RPC call might take a moment.
        event.deferReply().queue();

        // Retrieve the command options.
        int amount = event.getOption("amount").getAsInt();
        String address = event.getOption("address").getAsString();

        // Basic validation on the amount.
        if (amount <= 0) {
            event.getHook()
                    .sendMessage("Amount must be greater than zero.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // Check if user has sufficient balance to withdraw
        long userID = event.getUser().getIdLong();
        long userBalance = bot.walletHandler.getBalance(userID);
        if (userBalance < amount) {
            event.getHook()
                    .sendMessageEmbeds(EmbedUtils.createError("You do not have enough funds to withdraw that amount."))
                    .queue();
            return;
        }

        try {
            // Validate the provided address using the node's validateaddress RPC command.
            if (!isAddressValid(address)) {
                event.getHook()
                        .sendMessageEmbeds(EmbedUtils.createError("The wallet address you provided is not valid."))
                        .queue();
                return;
            }

            // Check the node's wallet balance using the getbalance RPC command.
            double faucet = getFaucetBalance();
            if (amount > faucet) {
                event.getHook()
                        .sendMessageEmbeds(EmbedUtils.createError("The bot has insufficient funds. Please contact an admin!"))
                        .queue();
                return;
            }

            // Execute the RPC call to withdraw funds.
            String txid = sendToAddressRPC(address, amount);

            // Update balance in database
            bot.walletHandler.setBalance(userID, userBalance - amount);

            // Send success message
            MessageEmbed embed = new EmbedBuilder()
                    .setAuthor(event.getUser().getEffectiveName(), null, event.getUser().getEffectiveAvatarUrl())
                    .setThumbnail("https://cdn-icons-png.flaticon.com/512/8815/8815105.png")
                    .setDescription("You successfully withdrew " + FlopBot.fLogoEmoji + " **" + FORMATTER.format(amount) + " FLOP**")
                    .addField("Transaction ID", "[Click to View Transaction](https://explorer.flopcoin.net/ext/gettx/" + txid + ")", false)
                    .addField("Destination Address", "`" + address + "`", false)
                    .setColor(EmbedColor.DEFAULT.color)
                    .build();
            event.getHook().sendMessageEmbeds(embed).queue();
        } catch (Exception e) {
            event.getHook()
                    .sendMessageEmbeds(EmbedUtils.createError("An error occurred while trying to withdraw. Please contact an admin!"))
                    .queue();
        }
    }

    /**
     * Validates the given address using the node's validateaddress RPC command.
     *
     * @param address The wallet address to validate.
     * @return true if the address is valid; false otherwise.
     * @throws Exception if the RPC call fails.
     */
    private boolean isAddressValid(String address) throws Exception {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("jsonrpc", "1.0");
        jsonRequest.put("id", "validateaddress");
        jsonRequest.put("method", "validateaddress");
        JSONArray params = new JSONArray();
        params.put(address);
        jsonRequest.put("params", params);

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
            throw new Exception("Address validation failed: " + errorObj.optString("message", "Unknown RPC error"));
        }

        JSONObject result = jsonResponse.getJSONObject("result");
        return result.optBoolean("isvalid", false);
    }

    /**
     * Retrieves the faucet's balance using the getbalance RPC command.
     *
     * @return The current wallet balance.
     * @throws Exception if the RPC call fails.
     */
    private double getFaucetBalance() throws Exception {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("jsonrpc", "1.0");
        jsonRequest.put("id", "getbalance");
        jsonRequest.put("method", "getbalance");
        // Some nodes require an empty array of parameters.
        JSONArray params = new JSONArray();
        jsonRequest.put("params", params);

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
            throw new Exception("Failed to get balance: " + errorObj.optString("message", "Unknown RPC error"));
        }

        return jsonResponse.getDouble("result");
    }

    /**
     * Sends a JSON‑RPC request to the Flopcoin Core node to withdraw funds.
     *
     * @param address The destination wallet address.
     * @param amount  The amount of FLOP to withdraw.
     * @return The transaction ID returned by the node.
     * @throws Exception if the RPC call fails or the node returns an error.
     */
    private String sendToAddressRPC(String address, int amount) throws Exception {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("jsonrpc", "1.0");
        jsonRequest.put("id", "withdraw");
        jsonRequest.put("method", "sendtoaddress");
        JSONArray params = new JSONArray();
        params.put(address);
        params.put(amount);
        jsonRequest.put("params", params);

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

        return jsonResponse.get("result").toString();
    }
}
