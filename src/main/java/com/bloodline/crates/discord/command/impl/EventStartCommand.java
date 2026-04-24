package com.bloodline.crates.discord.command.impl;

import com.bloodline.crates.audit.AuditEntry;
import com.bloodline.crates.discord.DiscordCommandContext;
import com.bloodline.crates.discord.DiscordPermissionTier;
import com.bloodline.crates.discord.command.DiscordCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class EventStartCommand implements DiscordCommand {
    @Override
    public String getName() {
        return "start";
    }

    @Override
    public String getDescription() {
        return "Start a crate event";
    }

    @Override
    public String getGroupName() {
        return "event";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.STRING, "event_name", "Event name", true),
            new OptionData(OptionType.INTEGER, "duration_minutes", "Duration in minutes", true)
        );
    }

    @Override
    public DiscordPermissionTier getRequiredTier() {
        return DiscordPermissionTier.ADMIN;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, DiscordCommandContext context) {
        DiscordCommandSupport.deferEphemeral(event);
        String eventName = DiscordCommandSupport.stringOption(event, "event_name");
        int durationMinutes = Math.max(1, DiscordCommandSupport.intOption(event, "duration_minutes", 1));
        context.getPlugin().getServer().getScheduler().runTask(context.getPlugin(), () -> {
            context.getPlugin().getDiscordBotManager().getAlertManager().alertCrateEvent(eventName, true);
            context.getPlugin().getAuditLogger().log(new AuditEntry(
                null,
                event.getUser().getName(),
                AuditEntry.AuditEventType.EDITOR_ACTION,
                eventName,
                "discord event start for " + durationMinutes + " minutes",
                System.currentTimeMillis()
            ));
            event.getHook().editOriginalEmbeds(
                context.getEmbeds().success("Event Started", "Started **" + eventName + "** for **" + durationMinutes + "** minute(s)."))
                .queue();
        });
    }
}
