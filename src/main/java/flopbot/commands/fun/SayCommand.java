package flopbot.commands.fun;

import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

public class SayCommand extends Command {

    public SayCommand(FlopBot bot) {
        super(bot);
        this.name = "say";
        this.description = "Makes the bot say what you tell it to.";
        this.category = Category.FUN;
        this.args.add(new OptionData(STRING, "message", "What the bot should say", true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String content = event.getOption("message", OptionMapping::getAsString);
        assert content != null;
        event.reply(content).queue();
    }
}
