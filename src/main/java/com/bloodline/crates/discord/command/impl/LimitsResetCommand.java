package com.bloodline.crates.discord.command.impl;

import com.bloodline.crates.audit.AuditEntry;
import com.bloodline.crates.discord.DiscordCommandContext;
import com.bloodline.crates.discord.DiscordPermissionTier;
import com.bloodline.crates.discord.command.DiscordCommand;
import com.bloodline.crates.limit.LimitType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Locale;

public class LimitsResetCommand implements DiscordCommand {
    @Override
    public String getName() {
        return "reset";
    }

    @Override
    public String getDescription() {
        return "Reset crate limits";
    }

    @Override
    public String getGroupName() {
        return "limits";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.STRING, "crate", "Crate id", true),
            new OptionData(OptionType.STRING, "type", "GLOBAL or PER_PLAYER", true)
        );
    }

    @Override
    public DiscordPermissionTier getRequiredTier() {
        return DiscordPermissionTier.ADMIN;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, DiscordCommandContext context) {
        DiscordCommandSupport.deferEphemeral(event);
        String crateId = DiscordCommandSupport.stringOption(event, "crate");
        String typeName = DiscordCommandSupport.stringOption(event, "type").toUpperCase(Locale.ROOT);

        context.getPlugin().getServer().getScheduler().runTask(context.getPlugin(), () -> {
            if (context.getPlugin().getConfigManager().getCrate(crateId).isEmpty()) {
                event.getHook().editOriginalEmbeds(context.getEmbeds().error("Unknown crate `" + crateId + "`.")).queue();
                return;
            }

            if ("GLOBAL".equals(typeName)) {
                context.getPlugin().getLimitManager().resetLimit(crateId, LimitType.GLOBAL);
            } else if ("PER_PLAYER".equals(typeName)) {
                context.getPlugin().getLimitManager().resetAllPlayerLimits(crateId);
            } else {
                event.getHook().editOriginalEmbeds(context.getEmbeds().error("Type must be `GLOBAL` or `PER_PLAYER`.")).queue();
                return;
            }

            context.getPlugin().getAuditLogger().log(new AuditEntry(
                null,
                event.getUser().getName(),
                AuditEntry.AuditEventType.CONFIG_CHANGED,
                crateId,
                "discord limits reset " + typeName,
                System.currentTimeMillis()
            ));
            event.getHook().editOriginalEmbeds(
                context.getEmbeds().success("Limits Reset", "Reset **" + typeName + "** limits for **" + crateId + "**."))
                .queue();
        });
    }
}
