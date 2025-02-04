package flopbot.commands.utility;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.util.embeds.EmbedColor;

/**
 * Command that displays all server roles.
 *
 * @author TechnoVision
 */
public class RolesCommand extends Command {

    public RolesCommand(FlopBot bot) {
        super(bot);
        this.name = "roles";
        this.description = "Display all server roles and member counts.";
        this.category = Category.UTILITY;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        StringBuilder content = new StringBuilder();
        for (Role role : event.getGuild().getRoles()) {
            if (!role.isManaged()) {
                content.append(role.getAsMention());
                content.append("\n");
            }
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(EmbedColor.DEFAULT.color)
                .setTitle("All Roles")
                .setDescription(content);
        event.replyEmbeds(embed.build()).queue();
    }
}
