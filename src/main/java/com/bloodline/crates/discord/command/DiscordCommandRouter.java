package com.bloodline.crates.discord.command;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.discord.DiscordBotManager;
import com.bloodline.crates.discord.DiscordCommandContext;
import com.bloodline.crates.discord.DiscordPermissionTier;
import com.bloodline.crates.discord.command.impl.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class DiscordCommandRouter {
    private final BloodlineCrates plugin;
    private final DiscordCommandContext context;
    private final Map<String, DiscordCommand> commands = new LinkedHashMap<>();
    private final Map<String, PendingButtonAction> buttonActions = new ConcurrentHashMap<>();

    public DiscordCommandRouter(BloodlineCrates plugin, DiscordCommandContext context) {
        this.plugin = plugin;
        this.context = context;
        register(new StatsCommand());
        register(new PlayerLookupCommand());
        register(new LeaderboardCommand());
        register(new ServerInfoCommand());
        register(new GiveKeyCommand());
        register(new GiveCrateCommand());
        register(new GiveVirtualCrateCommand());
        register(new CooldownClearCommand());
        register(new LimitsResetCommand());
        register(new BroadcastCommand());
        register(new EventStartCommand());
        register(new EventStopCommand());
        register(new ReloadCommand());
    }

    public net.dv8tion.jda.api.interactions.commands.build.CommandData buildRootCommand() {
        Map<String, SubcommandGroupData> groups = new LinkedHashMap<>();
        List<SubcommandData> rootSubs = new ArrayList<>();

        for (DiscordCommand command : new LinkedHashSet<>(commands.values())) {
            SubcommandData data = new SubcommandData(command.getName(), command.getDescription());
            command.getOptions().forEach(data::addOptions);
            if (command.getGroupName() == null) {
                rootSubs.add(data);
            } else {
                groups.computeIfAbsent(command.getGroupName(),
                    group -> new SubcommandGroupData(group, group.substring(0, 1).toUpperCase() + group.substring(1) + " commands"))
                    .addSubcommands(data);
            }
        }

        return Commands.slash("bloodcrates", "BloodlineCrates Discord commands")
            .addSubcommands(rootSubs)
            .addSubcommandGroups(groups.values());
    }

    public void handleSlash(SlashCommandInteractionEvent event) {
        if (!"bloodcrates".equalsIgnoreCase(event.getName())) {
            return;
        }

        String commandChannelId = context.getBotManager().getDiscordConfig().getCommandChannelId();
        if (commandChannelId != null && !commandChannelId.equals(event.getChannel().getId())) {
            event.replyEmbeds(context.getEmbeds().error("Commands are restricted to <#" + commandChannelId + ">.")).setEphemeral(true).queue();
            return;
        }

        String key = key(event.getSubcommandGroup(), event.getSubcommandName());
        DiscordCommand command = commands.get(key);
        if (command == null) {
            event.replyEmbeds(context.getEmbeds().error("Unknown command.")).setEphemeral(true).queue();
            return;
        }

        if (context.getRateLimiter().isRateLimited(event.getUser().getId())) {
            long remaining = context.getRateLimiter().getRemainingCooldownSeconds(event.getUser().getId());
            event.replyEmbeds(context.getEmbeds().error("Rate limited. Try again in " + remaining + "s.")).setEphemeral(true).queue();
            return;
        }

        if (!context.getPermissionManager().hasPermission(event.getMember(), command.getRequiredTier())) {
            event.replyEmbeds(context.getEmbeds().error("You don't have permission to use this command.")).setEphemeral(true).queue();
            return;
        }

        context.getRateLimiter().recordUsage(event.getUser().getId());
        try {
            command.execute(event, context);
        } catch (Exception exception) {
            event.replyEmbeds(context.getEmbeds().error("An error occurred while executing the command.")).setEphemeral(true).queue();
            plugin.getLogger().severe("Discord command failure for " + key + ": " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    public void handleButton(ButtonInteractionEvent event) {
        PendingButtonAction action = buttonActions.remove(event.getComponentId());
        if (action == null) {
            event.replyEmbeds(context.getEmbeds().error("This action has expired.")).setEphemeral(true).queue();
            return;
        }
        if (!action.discordUserId().equals(event.getUser().getId())) {
            event.replyEmbeds(context.getEmbeds().error("That confirmation belongs to someone else.")).setEphemeral(true).queue();
            return;
        }
        action.action().accept(event);
    }

    public List<Button> createConfirmationButtons(String discordUserId, Consumer<ButtonInteractionEvent> confirmAction, Consumer<ButtonInteractionEvent> cancelAction) {
        String confirmId = "bc:confirm:" + UUID.randomUUID();
        String cancelId = "bc:cancel:" + UUID.randomUUID();
        buttonActions.put(confirmId, new PendingButtonAction(discordUserId, confirmAction));
        buttonActions.put(cancelId, new PendingButtonAction(discordUserId, cancelAction));
        return List.of(
            Button.success(confirmId, "Confirm"),
            Button.danger(cancelId, "Cancel")
        );
    }

    public DiscordCommand getCommand(String name, String group) {
        return commands.get(key(group, name));
    }

    private void register(DiscordCommand command) {
        commands.put(key(command.getGroupName(), command.getName()), command);
    }

    private String key(String group, String name) {
        return (group == null ? "" : group + ":") + name;
    }

    private record PendingButtonAction(String discordUserId, Consumer<ButtonInteractionEvent> action) {
    }
}
