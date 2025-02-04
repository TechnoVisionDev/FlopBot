package flopbot.commands.links;

import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.util.embeds.EmbedColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class ExplorerCommand extends Command {

    public ExplorerCommand(FlopBot bot) {
        super(bot);
        this.name = "explorer";
        this.description = "Access links to the block explorer.";
        this.category = Category.LINKS;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Flopcoin Block Explorer")
                .setThumbnail("https://i.imgur.com/ErgbjAi.png")
                .setDescription("Check out the Flopcoin block explorer!\nhttps://explorer.flopcoin.net")
                .setColor(EmbedColor.DEFAULT.color)
                .build();
        event.replyEmbeds(embed).queue();
    }
}
