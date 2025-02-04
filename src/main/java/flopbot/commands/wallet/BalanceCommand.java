package flopbot.commands.wallet;

import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.util.embeds.EmbedColor;
import flopbot.util.embeds.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.io.IOException;

import static flopbot.util.NumberFormat.formatDouble;

/**
 * Command to check wallet balance.
 *
 * @author TechnoVision
 */
public class BalanceCommand extends Command {

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
            String valueInDollars = formatDouble(bot.walletHandler.getValueInDollars(balance));
            String formattedBalance = FlopBot.COIN_EMOJI + " **" + formatDouble(balance) + " FLOP** (≈ $" + valueInDollars + ")";

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
}

