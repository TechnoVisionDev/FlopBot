package flopbot.commands.wallet;

import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.util.embed.EmbedColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

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

        String name = event.getUser().getEffectiveName();
        String balance = FlopBot.fLogoEmoji + " **1,742.14 FLOP** (≈ $0.00)";
        String address = "`FLnCDfPKHvX5t4jbtAGVvuNdMYB18ZGYRp`";

        MessageEmbed embed = new EmbedBuilder()
                .setTitle(name+"'s Wallet")
                .addField("Balance:", balance, false)
                .addField("Wallet Address:", address, false)
                .setThumbnail(event.getUser().getAvatarUrl())
                .setColor(EmbedColor.DEFAULT.color)
                .build();

        event.replyEmbeds(embed).queue();
    }
}
