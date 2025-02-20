package flopbot.commands;

/**
 * Category that represents a group of similar commands.
 * Each category has a name and an emoji.
 *
 * @author TechnoVision
 */
public enum Category {
    FAUCET(":ocean: ", "Faucet"),
    UTILITY(":tools: ", "Utility"),
    STAKING(":bar_chart:", "Staking"),
    CASINO(":game_die:", "Casino"),
    WALLET(":moneybag:", "Wallet"),
    LINKS(":globe_with_meridians:", "Links");

    public final String emoji;
    public final String name;

    Category(String emoji, String name) {
        this.emoji = emoji;
        this.name = name;
    }
}
