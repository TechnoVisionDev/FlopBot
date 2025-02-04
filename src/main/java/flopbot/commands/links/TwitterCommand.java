package flopbot.commands.links;

import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.util.embed.EmbedColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class TwitterCommand extends Command {

    public TwitterCommand(FlopBot bot) {
        super(bot);
        this.name = "twitter";
        this.description = "Access links to the official twitter.";
        this.category = Category.LINKS;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Flopcoin Twitter")
                .setThumbnail("https://cdn4.iconfinder.com/data/icons/social-media-icons-the-circle-set/48/twitter_circle-512.png")
                .setDescription("Check out our official Twitter!\nhttps://x.com/flopcoin_dev")
                .setColor(EmbedColor.DEFAULT.color)
                .build();
        event.replyEmbeds(embed).queue();
    }
}
