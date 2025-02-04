package flopbot.commands.casino;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.handlers.WalletHandler;
import flopbot.util.embeds.EmbedColor;
import flopbot.util.embeds.EmbedUtils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class SlotsCommand extends Command {

    private final Map<Long, String> USER_THEMES;
    private final Map<String, List<String>> THEMES;

    public SlotsCommand(FlopBot bot) {
        super(bot);
        this.name = "slots";
        this.description = "Spin the slot machine.";
        this.category = Category.CASINO;
        this.args.add(new OptionData(OptionType.INTEGER, "bet", "The amount you want to wager", true).setMinValue(1));
        this.args.add(new OptionData(OptionType.STRING, "theme", "The theme of the slot machine")
                .addChoice("fruity", "fruity")
                .addChoice("luxury", "luxury")
                .addChoice("space", "space")
                .addChoice("desert", "desert")
                .addChoice("enchanted", "enchanted")
                .addChoice("sexy", "sexy")
                .addChoice("holiday", "holiday")
                .addChoice("drunk", "drunk")
                .addChoice("apocalypse", "apocalypse")
                .addChoice("spooky", "spooky"));

        // Store themed slot emojis
        USER_THEMES = new HashMap<>();
        THEMES = new HashMap<>();
        THEMES.put("fruity", List.of("\uD83C\uDF4E", "\uD83C\uDF47", "\uD83C\uDF4C"));
        THEMES.put("luxury", List.of("\uD83D\uDCB8", "\uD83D\uDCB0", "\uD83D\uDC8E"));
        THEMES.put("space", List.of("\uD83E\uDE90", "\uD83D\uDE80", "\uD83D\uDC7D"));
        THEMES.put("desert", List.of("\uD83C\uDF35", "\uD83D\uDC2A", "☀"));
        THEMES.put("enchanted", List.of("✨", "\uD83C\uDF44", "\uD83E\uDDDA"));
        THEMES.put("sexy", List.of("\uD83D\uDCA6", "\uD83C\uDF51", "\uD83C\uDF46"));
        THEMES.put("holiday", List.of("❄", "\uD83C\uDF85", "\uD83C\uDF84"));
        THEMES.put("drunk", List.of("\uD83C\uDF78", "\uD83C\uDF7A", "\uD83C\uDF7E"));
        THEMES.put("apocalypse", List.of("\uD83D\uDD25", "\uD83C\uDF2A", "\uD83C\uDF0B"));
        THEMES.put("spooky", List.of("\uD83D\uDD78", "\uD83C\uDF83", "☠"));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Get command data and theme
        User user = event.getUser();
        long bet = event.getOption("bet").getAsLong();
        OptionMapping themeOption = event.getOption("theme");
        List<String> emojis;
        if (themeOption != null) {
            String themeName = themeOption.getAsString();
            emojis = THEMES.get(themeName);
            USER_THEMES.put(user.getIdLong(), themeName);
        } else {
            String themeName = USER_THEMES.get(user.getIdLong());
            if (themeName == null) themeName = "fruity";
            emojis = THEMES.get(themeName);
        }

        // Charge player for bet
        WalletHandler walletHandler = bot.walletHandler;
        long balance = walletHandler.getBalance(user.getIdLong());
        if (balance < bet) {
            String currency = FlopBot.COIN_EMOJI + " **" + balance + "**";
            String text = "You don't have enough money for this bet. You currently have " + currency + " in cash.";
            event.replyEmbeds(EmbedUtils.createError(text)).setEphemeral(true).queue();
            return;
        }
        walletHandler.removeCoins(user.getIdLong(), bet);

        // Spin slot machine
        int[] slot = new int[3]; // will hold the middle row's random numbers

        StringBuilder slotMachine = new StringBuilder();
        // We are generating 3 rows
        for (int i = 0; i < 3; i++) {
            // Generate an array of 3 random numbers in the range [0, 3)
            int[] row = ThreadLocalRandom.current().ints(3, 0, 3).toArray();
            // Build a string representation for this row using the chosen emojis
            String rowStr = Arrays.stream(row)
                    .mapToObj(emojis::get)
                    .collect(Collectors.joining(" | "));
            // Save the middle row for outcome calculation and mark it with an arrow
            if (i == 1) {
                slot = row;
                rowStr += " ⬅";
            }
            slotMachine.append(rowStr).append("\n");
        }

        // Calculate earnings: win if all 3 numbers in the middle row are the same
        boolean isWinner = slot[0] == slot[1] && slot[0] == slot[2];
        long earnings = bet;
        if (isWinner) {
            earnings = bet * 9;
        }

        // Build the embed message
        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(user.getEffectiveName(), null, user.getEffectiveAvatarUrl());
        if (isWinner) {
            embed.setColor(EmbedColor.SUCCESS.color);
            embed.setDescription("You won " + FlopBot.COIN_EMOJI + " " + earnings + "\n\n");
            walletHandler.sendCoins(user.getIdLong(), earnings);
        } else {
            embed.setColor(EmbedColor.ERROR.color);
            embed.setDescription("You lost " + FlopBot.COIN_EMOJI + " " + bet + "\n\n");
        }
        embed.appendDescription(slotMachine);
        event.replyEmbeds(embed.build()).queue();
    }
}
