package flopbot.commands.links;

import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.util.embeds.EmbedColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class GithubCommand extends Command {

    public GithubCommand(FlopBot bot) {
        super(bot);
        this.name = "github";
        this.description = "Access links to the Github repo.";
        this.category = Category.LINKS;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Flopcoin Github Repo")
                .setThumbnail("https://pngimg.com/d/github_PNG72.png")
                .setDescription("Check out the Flopcoin Github repo!\nhttps://github.com/Flopcoin/Flopcoin")
                .setColor(EmbedColor.DEFAULT.color)
                .build();
        event.replyEmbeds(embed).queue();
    }
}
