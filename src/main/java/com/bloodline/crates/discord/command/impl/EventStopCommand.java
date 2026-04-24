package com.bloodline.crates.discord.command.impl;

import com.bloodline.crates.audit.AuditEntry;
import com.bloodline.crates.discord.DiscordCommandContext;
import com.bloodline.crates.discord.DiscordPermissionTier;
import com.bloodline.crates.discord.command.DiscordCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class EventStopCommand implements DiscordCommand {
    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public String getDescription() {
        return "Stop a crate event";
    }

    @Override
    public String getGroupName() {
        return "event";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(new OptionData(OptionType.STRING, "event_name", "Event name", true));
    }

    @Override
    public DiscordPermissionTier getRequiredTier() {
        return DiscordPermissionTier.ADMIN;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, DiscordCommandContext context) {
        DiscordCommandSupport.deferEphemeral(event);
        String eventName = DiscordCommandSupport.stringOption(event, "event_name");
        context.getPlugin().getServer().getScheduler().runTask(context.getPlugin(), () -> {
            context.getPlugin().getDiscordBotManager().getAlertManager().alertCrateEvent(eventName, false);
            context.getPlugin().getAuditLogger().log(new AuditEntry(
                null,
                event.getUser().getName(),
                AuditEntry.AuditEventType.EDITOR_ACTION,
                eventName,
                "discord event stop",
                System.currentTimeMillis()
            ));
            event.getHook().editOriginalEmbeds(context.getEmbeds().success("Event Stopped", "Stopped **" + eventName + "**.")).queue();
        });
    }
}
