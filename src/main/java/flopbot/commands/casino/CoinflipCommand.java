package flopbot.commands.casino;

import flopbot.util.NumberFormat;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.handlers.WalletHandler;
import flopbot.util.embeds.EmbedColor;
import flopbot.util.embeds.EmbedUtils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Command that plays a coinflip casino game.
 *
 * @author TechnoVision
 */
public class CoinflipCommand extends Command {

    public CoinflipCommand(FlopBot bot) {
        super(bot);
        this.name = "coinflip";
        this.description = "Flip a coin and bet on heads or tails.";
        this.category = Category.CASINO;
        this.args.add(new OptionData(OptionType.STRING, "choice", "The side you think the coin will land on", true)
                .addChoice("heads", "heads")
                .addChoice("tails", "tails"));
        this.args.add(new OptionData(OptionType.INTEGER, "bet", "The amount you want to wager", true).setMinValue(1));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Get command data
        User user = event.getUser();
        String choice = event.getOption("choice").getAsString();
        long bet = event.getOption("bet").getAsLong();

        // Charge player for bet
        WalletHandler walletHandler = bot.walletHandler;
        long balance = walletHandler.getBalance(user.getIdLong());
        if (balance < bet) {
            String currency = FlopBot.COIN_EMOJI + " **" + balance + "**";
            String text = "You don't have enough money for this bet. You currently have " + currency + " in cash.";
            event.replyEmbeds(EmbedUtils.createError(text)).setEphemeral(true).queue();
            return;
        }
        walletHandler.removeCoins(user.getIdLong(), bet);

        // Flip coin and calculate result
        EmbedBuilder embed = new EmbedBuilder();
        int result = ThreadLocalRandom.current().nextInt(2);
        String winnings = FlopBot.COIN_EMOJI + " " + NumberFormat.FORMATTER.format(bet*2);
        String losings = FlopBot.COIN_EMOJI + " -" + NumberFormat.FORMATTER.format(bet);
        if (result == 0) {
            embed.setAuthor("Heads!", null, user.getEffectiveAvatarUrl());
            if (choice.equalsIgnoreCase("heads")) {
                embed.setColor(EmbedColor.SUCCESS.color);
                embed.setDescription("Congratulations, the coin landed on heads!\nYou won " + winnings);
                walletHandler.sendCoins(user.getIdLong(), bet*2);
            } else {
                embed.setColor(EmbedColor.ERROR.color);
                embed.setDescription("Sorry, the coin landed on heads.\nYou lost " + losings);
            }
        } else {
            embed.setAuthor("Tails!", null, user.getEffectiveAvatarUrl());
            if (choice.equalsIgnoreCase("tails")) {
                embed.setColor(EmbedColor.SUCCESS.color);
                embed.setDescription("Congratulations, the coin landed on tails!\nYou won " + winnings);
                walletHandler.sendCoins(user.getIdLong(), bet*2);
            } else {
                embed.setColor(EmbedColor.ERROR.color);
                embed.setDescription("Sorry, the coin landed on tails.\nYou lost " + losings);
            }
        }

        // Send message response
        event.reply(":coin: The coin flips into the air...").queue(msg -> {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    msg.editOriginalEmbeds(embed.build()).queue();
                }
            }, 2500L);
        });
    }
}
