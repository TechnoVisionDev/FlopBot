package flopbot.handlers;

import flopbot.FlopBot;
import flopbot.commands.casino.BlackjackCommand;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Listens for button input and handles all button backend.
 *
 * @author TechnoVision
 */
public class ButtonHandler extends ListenerAdapter {

    public static final int MINUTES_TO_DISABLE = 3;

    public static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(20);
    public static final Map<String, List<MessageEmbed>> menus = new HashMap<>();
    public static final Map<String, List<Button>> buttons = new HashMap<>();

    private FlopBot bot;

    public ButtonHandler(FlopBot bot) {
        this.bot = bot;
    }

    /**
     * Adds pagination buttons to a message action.
     *
     * @param userID the ID of the user who is accessing this menu.
     * @param action the ReplyCallbackAction to add components to.
     * @param embeds the embed pages.
     */
    public static void sendPaginatedMenu(String userID, ReplyCallbackAction action, List<MessageEmbed> embeds) {
        String uuid = userID + ":" + UUID.randomUUID();
        List<Button> components = getPaginationButtons(uuid, embeds.size());
        buttons.put(uuid, components);
        menus.put(uuid, embeds);
        action.addActionRow(components).queue(interactionHook -> ButtonHandler.disableButtons(uuid, interactionHook));
    }

    /**
     * Get a list of buttons for paginated embeds.
     *
     * @param uuid the unique ID generated for these buttons.
     * @param maxPages the total number of embed pages.
     * @return A list of components to use on a paginated embed.
     */
    private static List<Button> getPaginationButtons(String uuid, int maxPages) {
        return Arrays.asList(
                Button.primary("pagination:prev:"+uuid, "Previous").asDisabled(),
                Button.of(ButtonStyle.SECONDARY, "pagination:page:0", "1/"+maxPages).asDisabled(),
                Button.primary("pagination:next:"+uuid, "Next")
        );
    }

    /**
     * Schedules a timer task to disable buttons and clear cache after a set time.
     *
     * @param uuid the uuid of the components to disable.
     * @param hook a interaction hook pointing to original message.
     */
    public static void disableButtons(String uuid, InteractionHook hook) {
        Runnable task = () -> {
            List<Button> actionRow = buttons.get(uuid);
            List<Button> newActionRow = new ArrayList<>();
            for (Button button : actionRow) {
                newActionRow.add(button.asDisabled());
            }
            hook.editOriginalComponents(ActionRow.of(newActionRow)).queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            buttons.remove(uuid);
            menus.remove(uuid);
        };
        executor.schedule(task, MINUTES_TO_DISABLE, TimeUnit.MINUTES);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        // Check that these are 'help' buttons
        String[] pressedArgs = event.getComponentId().split(":");

        // Check if user owns this menu
        long userID = Long.parseLong(pressedArgs[2]);
        if (userID != event.getUser().getIdLong()) return;

        // Get other buttons
        String uuid = userID+":"+pressedArgs[3];
        List<Button> components = buttons.get(uuid);
        if (components == null) return;
        String[] storedArgs = components.get(0).getId().split(":");

        if (pressedArgs[0].equals("pagination") && storedArgs[0].equals("pagination")) {
            if (pressedArgs[1].equals("next")) {
                // Move to next embed
                int page = Integer.parseInt(components.get(1).getId().split(":")[2]) + 1;
                List<MessageEmbed> embeds = menus.get(uuid);
                if (page < embeds.size()) {
                    // Update buttons
                    components.set(1, components.get(1).withId("pagination:page:" + page).withLabel((page + 1) + "/" + embeds.size()));
                    components.set(0, components.get(0).asEnabled());
                    if (page == embeds.size() - 1) {
                        components.set(2, components.get(2).asDisabled());
                    }
                    buttons.put(uuid, components);
                    event.editComponents(ActionRow.of(components)).setEmbeds(embeds.get(page)).queue();
                }
            } else if (pressedArgs[1].equals("prev")) {
                // Move to previous embed
                int page = Integer.parseInt(components.get(1).getId().split(":")[2]) - 1;
                List<MessageEmbed> embeds = menus.get(uuid);
                if (page >= 0) {
                    // Update buttons
                    components.set(1, components.get(1).withId("pagination:page:" + page).withLabel((page + 1) + "/" + embeds.size()));
                    components.set(2, components.get(2).asEnabled());
                    if (page == 0) {
                        components.set(0, components.get(0).asDisabled());
                    }
                    buttons.put(uuid, components);
                    event.editComponents(ActionRow.of(components)).setEmbeds(embeds.get(page)).queue();
                }
            }
        }
        else if (pressedArgs[0].equals("blackjack") && storedArgs[0].equals("blackjack")) {
            long bet = Long.parseLong(pressedArgs[4]);
            MessageEmbed embed = null;
            if (pressedArgs[1].equals("hit")) {
                embed = BlackjackCommand.hit(bot.walletHandler, event.getUser(), bet, uuid);
            } else if (pressedArgs[1].equals("stand")) {
                embed = BlackjackCommand.stand(bot.walletHandler, event.getUser(), bet, uuid);
            }
            event.editComponents(ActionRow.of(buttons.get(uuid))).setEmbeds(embed).queue();
        }
    }
}
