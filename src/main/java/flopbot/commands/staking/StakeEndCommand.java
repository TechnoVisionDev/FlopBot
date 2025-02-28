package flopbot.commands.staking;

import com.mongodb.client.model.Filters;
import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.data.cache.Stake;
import flopbot.util.embeds.EmbedUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class StakeEndCommand extends Command {

    public StakeEndCommand(FlopBot bot) {
        super(bot);
        this.name = "end";
        this.description = "End an active stake.";
        this.category = Category.STAKING;
        this.args.add(new OptionData(OptionType.STRING, "txid", "The transaction ID of an active stake", true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        long userId = Long.parseLong(event.getUser().getId());
        String txid = event.getOption("txid").getAsString();

        // Find the stake with the specified TXID that belongs to the user.
        Stake stake = bot.database.stakes.find(
                Filters.and(
                        Filters.eq("userId", userId),
                        Filters.eq("txid", txid)
                )
        ).first();

        if (stake == null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError("No active stake found with the provided TXID.")).queue();
            return;
        }

        // Delete stakes
        bot.database.stakes.deleteOne(Filters.eq("_id", stake.getId()));

        event.replyEmbeds(EmbedUtils.createSuccess("You have canceled your stake! You can now spent these funds!")).queue();
    }
}
