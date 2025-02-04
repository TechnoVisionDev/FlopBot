package flopbot.commands.casino;

import flopbot.FlopBot;
import flopbot.commands.Category;
import flopbot.commands.Command;
import flopbot.handlers.ButtonHandler;
import flopbot.handlers.WalletHandler;
import flopbot.util.embeds.EmbedColor;
import flopbot.util.embeds.EmbedUtils;
import flopbot.util.enums.Cards;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Command that plays a game of blackjack.
 *
 * @author TechnoVision
 */
public class BlackjackCommand extends Command {

    public static final String CARDBACK_EMOJI = "<:cardback:1336232629019742298>";
    public static final Map<String, Blackjack> games = new HashMap<>();
    public static final Map<String, Stack<Cards>> decks = new HashMap<>();
    public static final Map<String, ScheduledFuture> resetTimers = new HashMap<>();

    public BlackjackCommand(FlopBot bot) {
        super(bot);
        this.name = "blackjack";
        this.description = "Play a game of blackjack.";
        this.category = Category.CASINO;
        this.args.add(new OptionData(OptionType.INTEGER, "bet", "The amount you want to wager", true).setMinValue(1));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Check if game already exists
        User user = event.getUser();
        String userID = user.getId();
        if (games.containsKey(userID)) {
            String text = "You are already playing a blackjack game!";
            event.replyEmbeds(EmbedUtils.createError(text)).setEphemeral(true).queue();
            return;
        }

        // Charge player for bet
        long bet = event.getOption("bet").getAsLong();
        WalletHandler walletHandler = bot.walletHandler;
        long balance = walletHandler.getBalance(user.getIdLong());
        if (balance < bet) {
            String currency = FlopBot.COIN_EMOJI + " **" + balance + "**";
            String text = "You don't have enough FLOP for this bet. You currently have " + currency + " available.";
            event.replyEmbeds(EmbedUtils.createError(text)).setEphemeral(true).queue();
            return;
        }
        walletHandler.removeCoins(user.getIdLong(), bet);

        // Create and shuffle draw deck
        Stack<Cards> deck = decks.get(userID);
        if (deck == null || deck.size() <= 15) {
            deck = new Stack<>();
            for (Cards card : Cards.values()) {
                deck.push(card);
            }
            Collections.shuffle(deck);
            decks.put(userID, deck);
        }

        // Create new blackjack game
        Cards dealerCard = deck.pop();
        List<Cards> playerHand = new ArrayList<>();
        playerHand.add(deck.pop());
        playerHand.add(deck.pop());
        games.put(user.getId(), new Blackjack(userID, dealerCard, playerHand));

        // Send embed with buttons
        int score = calculateValue(playerHand);
        MessageEmbed embed = getEmbed(user, score).build();
        String uuid = user.getId() + ":" + UUID.randomUUID();
        List<Button> buttons = List.of(Button.primary("blackjack:hit:"+uuid+":"+bet, "Hit"), Button.secondary("blackjack:stand:"+uuid+":"+bet, "Stand"));
        ButtonHandler.buttons.put(uuid, buttons);
        event.replyEmbeds(embed).addActionRow(buttons).queue(interactionHook -> {
            // Delete all game data if no response for 3 min
            ButtonHandler.disableButtons(uuid, interactionHook);
            resetTimers.put(userID, ButtonHandler.executor.schedule(() -> {
                decks.remove(userID);
                games.remove(userID);
                resetTimers.remove(userID);
            }, 3, TimeUnit.MINUTES));
        });
    }

    /**
     * Stores and represents a blackjack game.
     *
     * @param dealerCard the card that the dealer has revealed.
     * @param playerHand List of cards that the player has.
     */
    public record Blackjack(String userID, Cards dealerCard, List<Cards> playerHand) {

        /**
         * Draws a card from the deck and adds it to the player's hand.
         */
        public void hit() {
            playerHand.add(decks.get(userID).pop());
        }
    }

    /**
     * Create a blackjack embed with current hand and score.
     *
     * @param user the user playing blackjack.
     * @param score the current score of the player's hand.
     * @return a MessageEmbed for the blackjack game.
     */
    public static EmbedBuilder getEmbed(User user, int score) {
        Blackjack game = games.get(user.getId());
        Cards dealerCard = game.dealerCard();
        List<Cards> playerHand = game.playerHand();

        String dealerText = dealerCard.emoji + " " + CARDBACK_EMOJI + "\n\nValue: " + dealerCard.value;
        String userText = printCards(playerHand) + "\n\nValue: " + score;
        return new EmbedBuilder()
                .setColor(EmbedColor.DEFAULT.color)
                .setAuthor(user.getEffectiveName(), null, user.getEffectiveAvatarUrl())
                .addField("Your Hand", userText, true)
                .addField("Dealer Hand", dealerText, true);
    }

    /**
     * Same as getEmbed except this also shows the dealer's resulting hand.
     *
     * @param user the user playing blackjack.
     * @param dealerHand the revealed cards in the dealer's hand.
     * @param playerScore the current score of the player's hand.
     * @param dealerScore the score of the cards in the dealer's hand.
     * @return a MessageEmbed for the blackjack game with dealer cards revealed.
     */
    public static EmbedBuilder getResultEmbed(User user, List<Cards> dealerHand, int playerScore, int dealerScore) {
        Blackjack game = games.get(user.getId());
        List<Cards> playerHand = game.playerHand();

        String userText = printCards(playerHand) + "\n\nValue: " + playerScore;
        String dealerText = printCards(dealerHand) + "\n\nValue: " + dealerScore;
        return new EmbedBuilder()
                .setColor(EmbedColor.DEFAULT.color)
                .setAuthor(user.getEffectiveName(), null, user.getEffectiveAvatarUrl())
                .addField("Your Hand", userText, true)
                .addField("Dealer Hand", dealerText, true);
    }

    /**
     * Dealer will draw cards from the deck following standard blackjack rules.
     *
     * @param game the instance of the blackjack game.
     * @param playerScore the current score of the player's hand.
     * @return a list of revealed cards in the dealer's hand.
     */
    private static List<Cards> revealDealerHand(Blackjack game, int playerScore) {
        List<Cards> dealerHand = new ArrayList<>();
        dealerHand.add(game.dealerCard());
        int dealerScore = game.dealerCard().value;
        do {
            Cards card = decks.get(game.userID()).pop();
            dealerScore += card.value;
            if (dealerScore > 21) {
                for (Cards curr : dealerHand) {
                    if (curr.isAce) {
                        dealerScore -= 10;
                        break;
                    }
                }
            }
            dealerHand.add(card);
        } while (dealerScore < playerScore && dealerScore < 17);
        return dealerHand;
    }

    /**
     * Draws a card from the deck and updates score.
     *
     * @param user the user playing blackjack.
     * @param bet the amount of money the player bet with.
     * @param uuid the UUID for the embed buttons.
     * @return a MessageEmbed with the updated game status.
     */
    public static MessageEmbed hit(WalletHandler walletHandler, User user, long bet, String uuid) {
        // Update game stats
        games.get(user.getId()).hit();
        Blackjack game = games.get(user.getId());
        int score = calculateValue(game.playerHand());

        // Send updated embed
        EmbedBuilder embed;
        String currency = FlopBot.COIN_EMOJI;
        if (score >= 21) {
            if (score > 21) {
                // Player busted
                List<Cards> dealerHand = List.of(game.dealerCard(), decks.get(game.userID()).pop());
                int dealerScore = calculateValue(dealerHand);
                embed = getResultEmbed(user, dealerHand, score, dealerScore);
                embed.setDescription("Result: Bust " + currency + " -" + bet);
                embed.setColor(EmbedColor.ERROR.color);
            } else {
                // Player has 21 cards
                List<Cards> dealerHand = revealDealerHand(game, score);
                int dealerScore = calculateValue(dealerHand);
                embed = getResultEmbed(user, dealerHand, score, dealerScore);
                if (dealerScore == 21) {
                    // Player and dealer tied
                    embed.setDescription("Result: Push, money back");
                    embed.setColor(EmbedColor.WARNING.color);
                    walletHandler.sendCoins(user.getIdLong(), bet);
                } else {
                    // Player wins
                    embed.setDescription("Result: Win " + currency + " " + (2*bet));
                    embed.setColor(EmbedColor.SUCCESS.color);
                    walletHandler.sendCoins(user.getIdLong(), (2*bet));
                }
            }
            endGame(user.getId(), uuid);
        } else {
            embed = getEmbed(user, score);
        }
        return embed.build();
    }

    /**
     * Ends the game and reveals dealer cards.
     *
     * @param user the user playing blackjack.
     * @param bet the amount of money the player bet with.
     * @param uuid the UUID for the embed buttons.
     * @return a MessageEmbed with the updated game status.
     */
    public static MessageEmbed stand(WalletHandler walletHandler, User user, long bet, String uuid) {
        // Get player stats
        Blackjack game = games.get(user.getId());
        int score = calculateValue(game.playerHand());

        // Get dealer stats
        List<Cards> dealerHand = revealDealerHand(game, score);
        int dealerScore = calculateValue(dealerHand);

        // Send updated embed
        String currency = FlopBot.COIN_EMOJI;
        EmbedBuilder embed = getResultEmbed(user, dealerHand, score, dealerScore);
        if (dealerScore > score) {
            if (dealerScore > 21) {
                // Dealer busted
                embed.setDescription("Result: Dealer bust " + currency + " " + (2*bet));
                embed.setColor(EmbedColor.SUCCESS.color);
                walletHandler.sendCoins(user.getIdLong(), (2*bet));
            } else {
                // Dealer wins
                embed.setDescription("Result: Loss " + currency + " -" + bet);
                embed.setColor(EmbedColor.ERROR.color);
            }
        } else if (dealerScore == score) {
            // Player and dealer tie (push)
            embed.setDescription("Result: Push, money back");
            embed.setColor(EmbedColor.WARNING.color);
            walletHandler.sendCoins(user.getIdLong(), bet);
        } else {
            // Player wins
            embed.setDescription("Result: Win " + currency + " " + (2*bet));
            embed.setColor(EmbedColor.SUCCESS.color);
            walletHandler.sendCoins(user.getIdLong(), (2*bet));
        }
        endGame(user.getId(), uuid);
        return embed.build();
    }

    /**
     * Ends the game by deleting stats and disabling the embed buttons.
     *
     * @param userID the ID of the user playing this game.
     * @param uuid the UUID for the embed buttons.
     */
    public static void endGame(String userID, String uuid) {
        games.remove(userID);
        resetTimers.remove(userID).cancel(true);
        List<Button> old = ButtonHandler.buttons.get(uuid);
        List<Button> components = new ArrayList<>();
        components.add(old.get(0).asDisabled());
        components.add(old.get(1).asDisabled());
        ButtonHandler.buttons.put(uuid, components);
    }

    /**
     * Prints playing cards into a string using their respective emojis.
     *
     * @param hand the list of card enums in this player's hand.
     * @return a string with all cards in emoji form.
     */
    public static String printCards(List<Cards> hand) {
        StringBuilder text = new StringBuilder();
        for (Cards card : hand) {
            text.append(card.emoji).append(" ");
        }
        return text.toString();
    }

    /**
     * Calculate the value of the cards in a player's hand.
     * Card values are based on the blackjack rule set.
     *
     * @param hand the list of card enums in this player's hand.
     * @return the integer value of the player's cards.
     */
    public static int calculateValue(List<Cards> hand) {
        int value = 0;
        for (Cards card : hand) {
            value += card.value;
        }
        if (value > 21) {
            for (Cards card : hand) {
                if (card.isAce) {
                    value -= 10;
                    break;
                }
            }
        }
        return value;
    }
}
