package flopbot.commands.wallet;

import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.util.embed.EmbedColor;
import flopbot.util.embed.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.io.IOException;
import java.text.DecimalFormat;

/**
 * Command to check wallet balance.
 *
 * @author TechnoVision
 */
public class BalanceCommand extends Command {

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    public BalanceCommand(FlopBot bot) {
        super(bot);
        this.name = "balance";
        this.description = "View your wallet balance.";
        this.category = Category.WALLET;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        try {
            User user = event.getUser();
            String name = user.getEffectiveName();

            // Get wallet data from database and API
            String address = bot.walletHandler.getAddress(user.getIdLong());
            double num = bot.walletHandler.getBalance(address);
            String valueInDollars = DECIMAL_FORMAT.format(bot.walletHandler.getValueInDollars(num));
            String balance = FlopBot.fLogoEmoji + " **" + formatDouble(num) + " FLOP** (≈ $" + valueInDollars + ")";

            // Create embed with wallet info
            MessageEmbed embed = new EmbedBuilder()
                    .setTitle(name + "'s Wallet")
                    .addField("Balance:", balance, false)
                    .addField("Wallet Address:", "`" + address + "`", false)
                    .setThumbnail(user.getAvatarUrl())
                    .setColor(EmbedColor.DEFAULT.color)
                    .build();
            event.getHook().editOriginalEmbeds(embed).queue();

        } catch (IOException e) {
            event.getHook().editOriginalEmbeds(EmbedUtils.createError("An error occurred. Please try again!")).queue();
        }
    }

    /**
     * Formats a double with comma grouping. If the number is an integer,
     * it will have no decimals; otherwise, it will have two decimals.
     *
     * Examples:
     *   1          -> "1"
     *   10         -> "10"
     *   100        -> "100"
     *   1000       -> "1,000"
     *   1000.5     -> "1,000.50"
     *   1000000    -> "1,000,000"
     *
     * @param value the number to format.
     * @return the formatted string.
     */
    public static String formatDouble(double value) {
        // Check if the value is an integer (no fractional part)
        if (value == 0) {
            return "0.00";
        }
        else if (value == Math.floor(value)) {
            DecimalFormat intFormat = new DecimalFormat("#,##0");
            return intFormat.format(value);
        } else {
            DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
            return decimalFormat.format(value);
        }
    }
}
