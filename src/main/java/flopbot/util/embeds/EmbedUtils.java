package flopbot.util.embeds;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class storing helpful methods for embeds.
 *
 * @author Technovision
 */
public class EmbedUtils {

    /**
     * Custom Emojis.
     */
    public static final String GREEN_TICK = "✅";
    public static final String BLUE_TICK = "<:blue_tick:800623774293819413>";
    public static final String RED_X = "<:red_x:800554807164665916>";
    public static final String BLUE_X = "<:blue_x:800623785736798228>";

    /**
     * Quickly creates a simple error embed.
     *
     * @param errorMessage message to be displayed.
     * @return completed error embed.
     */
    public static @NotNull MessageEmbed createError(String errorMessage) {
        return new EmbedBuilder()
                .setColor(EmbedColor.ERROR.color)
                .setDescription(":x: " + errorMessage)
                .build();
    }

    /**
     * Quickly creates a simple default embed.
     *
     * @param message text to be displayed.
     * @return completed default embed.
     */
    public static @NotNull MessageEmbed createDefault(String message) {
        return new EmbedBuilder()
                .setColor(EmbedColor.DEFAULT.color)
                .setDescription(message)
                .build();
    }

    /**
     * Quickly creates a simple success embed.
     *
     * @param message text to be displayed.
     * @return completed success embed.
     */
    public static @NotNull MessageEmbed createSuccess(String message) {
        return new EmbedBuilder()
                .setColor(EmbedColor.SUCCESS.color)
                .setDescription(GREEN_TICK + " " + message)
                .build();
    }

}