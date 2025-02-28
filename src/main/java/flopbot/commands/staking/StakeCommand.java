package flopbot.commands.staking;

import flopbot.FlopBot;
import flopbot.commands.Command;
import flopbot.commands.Category;
import flopbot.util.embeds.EmbedColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public class StakeCommand extends Command {

    public static final MessageEmbed stakeHelpEmbed = new EmbedBuilder()
                .setTitle("How to Stake Flopcoin")
                .setColor(EmbedColor.DEFAULT.color)
                .setDescription("You can stake your coins by locking them in a UXTO and holding them without spending. You can earn daily rewards for these stakes proportional to the total amount staked. The reward pool is 20% of the total faucet balance.")
                .addField("Step 1: Create a UTXO", "Open your [Flopcoin Core Wallet](https://github.com/Flopcoin/Flopcoin/releases) and send any amount of coins to YOURSELF. Do NOT send them to a different wallet. The amount of coins you send to yourself will determine the amount that is staked.", false)
                .addField("Step 2: Lock the UTXO You Created", "To prevent your UXTO from accidentally being spent, go to your core wallet and \"enable coin control features\" under preferences->wallet. Next head to the \"Send\" tab and click the \"Inputs\" button. Find the transaction you made and right click it to \"Lock\" the funds. You can unlock it again at any time!", false)
                .addField("Step 3: Create a New Stake", "Use the `/stake new [txid] [amount]` command to create a new stake. Make sure the TXID and amount match the transaction you just created. You must stake within 10 confirmations of creating the UTXO.", false)
                .addField("Step 4: Claim Rewards Every Day", "Use the `/stake claim [txid]` to claim stake rewards every day. Rewards will be sent to your balance on FlopBot, which can be viewed using the `/balance` command and withdrawn at any time!", false)
                .addField("Step 5: Manage Active Stakes", "If your stake UTXO is spent or tampered with, your stake will be automatically removed. You can keep track of your active stakes with the `/stake list` command.", false)
                .addField("Step 5: End Stakes", "You can now use the `/stake end [txid]` command to end your stake at any time, which will remove them from the active list of tracked stakes.", false)
                .build();

    public StakeCommand(FlopBot bot) {
        super(bot);
        this.name = "stake";
        this.description = "Stake commands: new, list, end";
        this.category = Category.STAKING;

        // Define subcommands:
        this.subCommands.add(new SubcommandData("help", "Learn how to stake your coins."));
        this.subCommands.add(new SubcommandData("stats", "Display server-wide stats for coin staking."));
        this.subCommands.add(new SubcommandData("new", "Stake your coins to earn rewards each month.")
                .addOptions(new OptionData(OptionType.STRING, "txid", "The transaction ID used for staking", true))
                .addOptions(new OptionData(OptionType.NUMBER, "amount", "The amount of FLOP to stake (max 500M)", true).setMinValue(1).setMaxValue(500000000))
        );
        this.subCommands.add(new SubcommandData("list", "Display all of your active stakes."));
        this.subCommands.add(new SubcommandData("end", "End an active stake.")
                .addOptions(new OptionData(OptionType.STRING, "txid", "The transaction ID of an active stake", true))
        );
        this.subCommands.add(new SubcommandData("claim", "Claim daily rewards for an active stake.")
                .addOptions(new OptionData(OptionType.STRING, "txid", "The transaction ID of an active stake", true))
        );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String subCommand = event.getSubcommandName();
        if (subCommand == null) {
            event.reply("Please specify a subcommand: new, list, or end").queue();
            return;
        }
        switch (subCommand) {
            case "new":
                new StakeNewCommand(bot).execute(event);
                break;
            case "list":
                new StakeListCommand(bot).execute(event);
                break;
            case "end":
                new StakeEndCommand(bot).execute(event);
                break;
            case "stats":
                new StakeStatsCommand(bot).execute(event);
                break;
            case "claim":
                new StakeClaimCommand(bot).execute(event);
                break;
            case "help":
                event.replyEmbeds(stakeHelpEmbed).queue();
                break;
            default:
                event.replyEmbeds(stakeHelpEmbed).queue();
        }
    }
}
