package flopbot.commands.links;

import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.util.embed.EmbedColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class RedditCommand extends Command {

    public RedditCommand(FlopBot bot) {
        super(bot);
        this.name = "reddit";
        this.description = "Access links to the official subreddit.";
        this.category = Category.LINKS;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Flopcoin Subreddit")
                .setThumbnail("https://www.iconpacks.net/icons/2/free-reddit-logo-icon-2436-thumb.png")
                .setDescription("Check out our official subreddit!\nhttps://www.reddit.com/r/Flopcoin")
                .setColor(EmbedColor.DEFAULT.color)
                .build();
        event.replyEmbeds(embed).queue();
    }
}
