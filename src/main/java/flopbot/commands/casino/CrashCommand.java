package flopbot.commands.casino;

import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.handlers.WalletHandler;
import flopbot.handlers.ButtonHandler;
import flopbot.util.NumberFormat;
import flopbot.util.embeds.EmbedColor;
import flopbot.util.embeds.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.*;
import java.util.concurrent.*;

/**
 * Command that plays the crash gambling game.
 *
 * @author TechnoVision
 */
public class CrashCommand extends Command {

    public static final HashMap<Long, CrashGame> games = new HashMap<>();
    private static final ScheduledExecutorService tasks = Executors.newScheduledThreadPool(10);

    public CrashCommand(FlopBot bot) {
        super(bot);
        this.name = "crash";
        this.description = "Bet against a multiplier that can crash at any moment.";
        this.category = Category.CASINO;
        this.args.add(new OptionData(OptionType.INTEGER, "bet", "The amount you want to wager", true)
                .setMinValue(1));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        User user = event.getUser();
        long bet = event.getOption("bet").getAsLong();

        // Check if the user is already playing a game.
        if (games.containsKey(user.getIdLong())) {
            event.replyEmbeds(EmbedUtils.createError("You are currently playing a game of crash!"))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // Check for sufficient funds.
        WalletHandler walletHandler = bot.walletHandler;
        long balance = walletHandler.getBalance(user.getIdLong());
        if (balance < bet) {
            String currency = FlopBot.COIN_EMOJI + " **" + balance + "**";
            String errorText = "You don't have enough money for this bet. You currently have " + currency + " in cash.";
            event.replyEmbeds(EmbedUtils.createError(errorText))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        walletHandler.removeCoins(user.getIdLong(), bet);

        // 3% chance of an instant crash.
        if (ThreadLocalRandom.current().nextDouble() <= 0.03) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
                    .setColor(EmbedColor.ERROR.color)
                    .setDescription("Your bet: " + FlopBot.COIN_EMOJI + " " + NumberFormat.FORMATTER.format(bet))
                    .addField("Crashed At", "x1.00", true)
                    .addField("Loss", FlopBot.COIN_EMOJI + " -" + NumberFormat.FORMATTER.format(bet), true);
            event.replyEmbeds(embed.build())
                    .addActionRow(Button.primary("cashout", "Cashout").asDisabled())
                    .queue();
            return;
        }

        // Determine maximum multiplier (capped at 30).
        double randomMultiplier = 0.01 + (0.99 / ThreadLocalRandom.current().nextDouble());
        double maxMultiplier = Math.min(randomMultiplier, 30);

        // Build initial embed.
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(EmbedColor.DEFAULT.color)
                .setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
                .setDescription("Your Bet: " + FlopBot.COIN_EMOJI + " " + NumberFormat.FORMATTER.format(bet))
                .addField("Multiplier", "x1.00", true)
                .addField("Profit", FlopBot.COIN_EMOJI + " " + NumberFormat.FORMATTER.format(bet), true);

        // Create a unique identifier for the cashout button.
        String uuid = user.getId() + ":" + UUID.randomUUID();
        Button cashoutButton = Button.primary("crash:cashout:" + uuid + ":" + bet, "Cashout");
        ButtonHandler.buttons.put(uuid, List.of(cashoutButton));

        // Send initial message and start game loop.
        event.replyEmbeds(embed.build()).addActionRow(cashoutButton).queue(msg -> {
            ScheduledFuture<?> task = tasks.scheduleAtFixedRate(() -> {
                CrashGame game = games.get(user.getIdLong());
                if (game == null) return; // Game may have ended via cashout.

                game.currMultiplier += 0.1;
                String multiplierString = "x" + String.format("%.2f", game.currMultiplier);

                if (game.currMultiplier >= game.maxMultiplier) {
                    // Game crashes.
                    EmbedBuilder crashEmbed = new EmbedBuilder()
                            .setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
                            .setColor(EmbedColor.ERROR.color)
                            .setDescription("Your bet: " + FlopBot.COIN_EMOJI + " " + NumberFormat.FORMATTER.format(bet))
                            .addField("Crashed At", multiplierString, true)
                            .addField("Loss", FlopBot.COIN_EMOJI + " -" + NumberFormat.FORMATTER.format(bet), true);
                    games.remove(user.getIdLong()).task.cancel(true);
                    Button disabledButton = ButtonHandler.buttons.remove(uuid).get(0).asDisabled();
                    msg.editOriginalEmbeds(crashEmbed.build()).setActionRow(disabledButton).queue();
                } else {
                    int profit = (int) (bet * game.currMultiplier);
                    EmbedBuilder updateEmbed = new EmbedBuilder()
                            .setColor(EmbedColor.DEFAULT.color)
                            .setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
                            .setDescription("Your bet: " + FlopBot.COIN_EMOJI + " " + NumberFormat.FORMATTER.format(bet))
                            .addField("Multiplier", multiplierString, true)
                            .addField("Profit", FlopBot.COIN_EMOJI + " " + NumberFormat.FORMATTER.format(profit), true);
                    msg.editOriginalEmbeds(updateEmbed.build()).queue();
                }
            }, 1500, 1500, TimeUnit.MILLISECONDS);

            games.put(user.getIdLong(), new CrashGame(task, 1.0, maxMultiplier, bet));
        });
    }

    /**
     * Cashes out a running crash game, awarding the profit.
     *
     * @param walletHandler the wallet handler to update the user's balance.
     * @param user          the user cashing out.
     * @return a MessageEmbed displaying the cashout result.
     */
    public static MessageEmbed cashout(WalletHandler walletHandler, User user) {
        CrashGame game = games.remove(user.getIdLong());
        game.task.cancel(true);

        int profit = (int) (game.bet * game.currMultiplier);
        walletHandler.sendCoins(user.getIdLong(), profit);

        String multiplierString = "x" + String.format("%.2f", game.currMultiplier);
        return new EmbedBuilder()
                .setColor(EmbedColor.SUCCESS.color)
                .setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
                .setDescription("Your bet: " + FlopBot.COIN_EMOJI + " " + NumberFormat.FORMATTER.format(game.bet))
                .addField("Multiplier", multiplierString, true)
                .addField("Win", FlopBot.COIN_EMOJI + " " + NumberFormat.FORMATTER.format(profit), true)
                .build();
    }

    /**
     * Represents a Crash game and stores game data.
     */
    public static class CrashGame {
        final ScheduledFuture<?> task;
        double currMultiplier;
        final double maxMultiplier;
        final long bet;

        /**
         * Constructs a CrashGame.
         *
         * @param task          the scheduled task for this game.
         * @param currMultiplier the current multiplier.
         * @param maxMultiplier the multiplier at which the game crashes.
         * @param bet           the bet amount.
         */
        public CrashGame(ScheduledFuture<?> task, double currMultiplier, double maxMultiplier, long bet) {
            this.task = task;
            this.currMultiplier = currMultiplier;
            this.maxMultiplier = maxMultiplier;
            this.bet = bet;
        }
    }
}
