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

            // Get wallet data from database and API (price is cached inside the wallet handler)
            double balance = bot.walletHandler.getBalance(user.getIdLong());
            String valueInDollars = DECIMAL_FORMAT.format(bot.walletHandler.getValueInDollars(balance));
            String formattedBalance = FlopBot.flopcoinEmoji + " **" + formatDouble(balance) + " FLOP** (≈ $" + valueInDollars + ")";

            // Create embed with wallet info
            MessageEmbed embed = new EmbedBuilder()
                    .setTitle(name + "'s Wallet")
                    .addField("Balance:", formattedBalance, true)
                    .setThumbnail(user.getEffectiveAvatarUrl())
                    .setColor(EmbedColor.DEFAULT.color)
                    .build();
            event.getHook().editOriginalEmbeds(embed).queue();

        } catch (IOException e) {
            event.getHook().editOriginalEmbeds(EmbedUtils.createError("An error occurred. Please try again!")).queue();
        }
    }

    public static String formatDouble(double value) {
        if (value == 0) {
            return "0.00";
        } else if (value == Math.floor(value)) {
            DecimalFormat intFormat = new DecimalFormat("#,##0");
            return intFormat.format(value);
        } else {
            DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
            return decimalFormat.format(value);
        }
    }
}

