package flopbot.commands.links;

import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.util.embeds.EmbedColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class PoolsCommand extends Command {

    public PoolsCommand(FlopBot bot) {
        super(bot);
        this.name = "pools";
        this.description = "Access mining pools that support Flopcoin.";
        this.category = Category.LINKS;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Flopcoin Mining Pools")
                .setThumbnail("https://cdn-icons-png.flaticon.com/512/9109/9109910.png")
                .setDescription("Check out the mining pools that support Flopcoin!\nhttps://miningpoolstats.stream/flopcoin")
                .setColor(EmbedColor.DEFAULT.color)
                .build();
        event.replyEmbeds(embed).queue();
    }
}
