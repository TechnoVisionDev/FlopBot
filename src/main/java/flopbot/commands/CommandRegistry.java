package flopbot.commands;

import flopbot.FlopBot;
import flopbot.commands.faucet.DonateCommand;
import flopbot.commands.faucet.FaucetCommand;
import flopbot.commands.fun.SayCommand;
import flopbot.commands.utility.HelpCommand;
import flopbot.commands.wallet.BalanceCommand;
import flopbot.commands.wallet.WithdrawCommand;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Registers, listens for, and executes slash commands.
 *
 * @author TechnoVision
 */
public class CommandRegistry extends ListenerAdapter {

    /** List of commands in the exact order registered */
    public static final List<Command> commands = new ArrayList<>();

    /** Map of command names to command objects */
    public static final Map<String, Command> commandsMap = new HashMap<>();

    public CommandRegistry(FlopBot bot) {
        // Create and map commands
        mapCommand(
                //Category: WALLET
                new BalanceCommand(bot),
                new WithdrawCommand(bot),
                //Category: FAUCET
                new FaucetCommand(bot),
                new DonateCommand(bot),
                //Category: FUN
                new SayCommand(bot),
                //Category: UTILITY
                new HelpCommand(bot) // MUST BE LAST COMMAND REGISTERED!
        );
    }

    /**
     * Adds a command to the static list and map.
     *
     * @param cmds a spread list of command objects.
     */
    private void mapCommand(Command ...cmds) {
        for (Command cmd : cmds) {
            commandsMap.put(cmd.name, cmd);
            commands.add(cmd);
        }
    }

    /**
     * Creates a list of CommandData for all commands.
     *
     * @return a list of CommandData to be used for registration.
     */
    public static List<CommandData> unpackCommandData() {
        // Register slash commands
        List<CommandData> commandData = new ArrayList<>();
        for (Command command : commands) {
            SlashCommandData slashCommand = Commands.slash(command.name, command.description).addOptions(command.args);
            if (!command.subCommands.isEmpty()) {
                slashCommand.addSubcommands(command.subCommands);
            }
            commandData.add(slashCommand);
        }
        return commandData;
    }

    /**
     * Runs whenever a slash command is run in Discord.
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // Get command by name and execute it
        Command cmd = commandsMap.get(event.getName());
        if (cmd != null) {
            cmd.execute(event);
        }
    }

    /**
     * Registers slash commands as guild commands.
     * NOTE: May change to global commands on release.
     *
     * @param event executes when a guild is ready.
     */
    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        // Register slash commands
        event.getGuild().updateCommands().addCommands(unpackCommandData()).queue();
    }
}
