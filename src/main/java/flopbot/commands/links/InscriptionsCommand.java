package flopbot.commands.links;

import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.util.embed.EmbedColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class InscriptionsCommand extends Command {

    public InscriptionsCommand(FlopBot bot) {
        super(bot);
        this.name = "inscriptions";
        this.description = "Access links to view blockchain inscriptions.";
        this.category = Category.LINKS;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Flopcoin Inscriptions")
                .setThumbnail("https://cdn-icons-png.flaticon.com/512/4127/4127219.png")
                .setDescription("Check out the latest Flopcoin inscriptions!\nhttps://flopinals.flopcoin.net")
                .setColor(EmbedColor.DEFAULT.color)
                .build();
        event.replyEmbeds(embed).queue();
    }
}
