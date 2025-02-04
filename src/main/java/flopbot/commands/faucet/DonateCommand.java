package flopbot.commands.faucet;

import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.util.NumberFormat;
import flopbot.util.embeds.EmbedColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

/**
 * Allows users to refill the faucet with FLOP donations.
 *
 * @author TechnoVision
 */
public class DonateCommand extends Command {

    public DonateCommand(FlopBot bot) {
        super(bot);
        this.name = "donate";
        this.description = "Donate FLOP to refill the faucet.";
        this.category = Category.FAUCET;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String faucetBalance = NumberFormat.formatDouble(bot.getFaucetBalance());
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Donate to the Flopcoin Faucet")
                .setThumbnail("https://pics.freeicons.io/uploads/icons/png/5401033321644160058-512.png")
                .addField("Faucet Balance", "**" + FlopBot.COIN_EMOJI + " " + faucetBalance + " FLOP**", false)
                .addField("Faucet Address", "`" + FlopBot.FAUCET_ADDRESS + "`", false)
                .setDescription("Donate Flopcoin (FLOP) to the address below.\nThis helps keep the faucet running for everyone!")
                .setColor(EmbedColor.DEFAULT.color)
                .build();
        event.replyEmbeds(embed).queue();
    }
}
