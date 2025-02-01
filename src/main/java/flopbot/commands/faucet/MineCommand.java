package flopbot.commands.faucet;

import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.util.embed.EmbedColor;
import flopbot.util.embed.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.TimeFormat;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Command that adds money to your balance.
 *
 * @author TechnoVision
 */
public class MineCommand extends Command {

    private static final long FAUCET_TIMEOUT = 14400000;
    private static final DecimalFormat FORMATTER = new DecimalFormat("#,###");

    private Map<Long, Long> timeouts;

    public MineCommand(FlopBot bot) {
        super(bot);
        this.name = "mine";
        this.description = "Earn free coins from the faucet.";
        this.category = Category.FAUCET;
        this.timeouts = new HashMap<>();
    }

    public void execute(SlashCommandInteractionEvent event) {
        long userID = event.getUser().getIdLong();
        Long timeout = timeouts.get(userID);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor(event.getUser().getEffectiveName(), null, event.getUser().getEffectiveAvatarUrl());
        embed.setColor(EmbedColor.SUCCESS.color);

        if (timeout != null && System.currentTimeMillis() < timeout) {
            // On timeout
            embed.setDescription(":stopwatch: You can next mine " + formatTimeout(timeout) + ".");
            embed.setColor(EmbedColor.ERROR.color);
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        } else {
            try {
                // Send random amount of coins between 100 - 10,000
                int randomNumber = ThreadLocalRandom.current().nextInt(100, 10000 + 1);
                bot.walletHandler.sendCoins(userID, BigDecimal.valueOf(randomNumber));
                timeouts.put(userID, System.currentTimeMillis() + FAUCET_TIMEOUT);

                // Send embed reply
                embed.setDescription("You successfully mined " + FlopBot.fLogoEmoji + " **" + FORMATTER.format(randomNumber) + " FLOP**!\nView your balance using the `/balance` command.");
                embed.setThumbnail("https://cdn-icons-png.flaticon.com/512/9109/9109910.png");
                event.replyEmbeds(embed.build()).queue();

            } catch (IOException e) {
                event.replyEmbeds(EmbedUtils.createError("An error occurred, please try again!")).setEphemeral(true).queue();
            }
        }
    }

    /**
     * Formats timeout timestamp into a string timestamp for embeds.
     *
     * @param timeout the timestamp in millis.
     * @return a string timeout formatted for embeds.
     */
    public @Nullable String formatTimeout(long timeout) {
        return TimeFormat.RELATIVE.format(timeout);
    }
}
