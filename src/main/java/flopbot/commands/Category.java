package flopbot.commands;

/**
 * Category that represents a group of similar commands.
 * Each category has a name and an emoji.
 *
 * @author TechnoVision
 */
public enum Category {
    FAUCET(":ocean: ", "Faucet"),
    FUN(":smile:", "Fun"),
    CASINO(":game_die:", "Casino");

    public final String emoji;
    public final String name;

    Category(String emoji, String name) {
        this.emoji = emoji;
        this.name = name;
    }
}
