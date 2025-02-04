package flopbot.commands.links;

import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.util.embed.EmbedColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class WebsiteCommand extends Command {

    public WebsiteCommand(FlopBot bot) {
        super(bot);
        this.name = "website";
        this.description = "Access links to the official website.";
        this.category = Category.LINKS;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Flopcoin Website")
                .setThumbnail("https://i.imgur.com/ErgbjAi.png")
                .setDescription("Check out our official website!\nhttps://flopcoin.net")
                .setColor(EmbedColor.DEFAULT.color)
                .build();
        event.replyEmbeds(embed).queue();
    }
}
