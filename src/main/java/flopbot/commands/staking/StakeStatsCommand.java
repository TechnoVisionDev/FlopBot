package flopbot.commands.staking;

import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.data.cache.Stake;
import flopbot.util.embeds.EmbedColor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class StakeStatsCommand extends Command {

    private static final DecimalFormat amountFormatter = new DecimalFormat("#,##0.##");

    public StakeStatsCommand(FlopBot bot) {
        super(bot);
        this.name = "stats";
        this.description = "Display server-wide stats for coin staking.";
        this.category = Category.STAKING;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        // Query the stakes collection for all stakes.
        List<Stake> stakes = new ArrayList<>();
        bot.database.stakes.find().into(stakes);

        double totalStaked = 0;
        for (Stake stake : stakes) {
            totalStaked += stake.getAmount();
        }
        double monthlyRewardPool = bot.getFaucetBalance() * 0.1;
        double dailyRewardPool = monthlyRewardPool / 30;

        MessageEmbed embed = new EmbedBuilder()
                .setColor(EmbedColor.DEFAULT.color)
                .setTitle("Flopcoin Staking Statistics")
                .setDescription("Server wide staking statistics")
                .addField("Total Amount Staked",  FlopBot.COIN_EMOJI + " " + amountFormatter.format(totalStaked) + " FLOP", false)
                .addField("Monthly Reward Pool", FlopBot.COIN_EMOJI + " " + amountFormatter.format(monthlyRewardPool) + " FLOP", false)
                .addField("Daily Reward Pool", FlopBot.COIN_EMOJI + " " + amountFormatter.format(dailyRewardPool) + " FLOP", false)
                .build();

        event.getHook().sendMessageEmbeds(embed).queue();
    }
}