package flopbot.commands.links;

import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.util.embed.EmbedColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class WhitepaperCommand extends Command {

    public WhitepaperCommand(FlopBot bot) {
        super(bot);
        this.name = "whitepaper";
        this.description = "Access links to the Flopcoin whitepaper.";
        this.category = Category.LINKS;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Flopcoin Whitepaper")
                .setThumbnail("https://cdn-icons-png.flaticon.com/512/2495/2495971.png")
                .setDescription("Check out the Flopcoin whitepaper!\nhttps://tinyurl.com/flopcoin-whitepaper")
                .setColor(EmbedColor.DEFAULT.color)
                .build();
        event.replyEmbeds(embed).queue();
    }
}
