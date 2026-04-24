package com.bloodline.crates.discord.command.impl;

import com.bloodline.crates.audit.AuditEntry;
import com.bloodline.crates.discord.DiscordCommandContext;
import com.bloodline.crates.discord.DiscordPermissionTier;
import com.bloodline.crates.discord.command.DiscordCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Optional;

public class CooldownClearCommand implements DiscordCommand {
    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getDescription() {
        return "Clear a player's crate cooldowns";
    }

    @Override
    public String getGroupName() {
        return "cooldown";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.STRING, "player", "Player name", true),
            new OptionData(OptionType.STRING, "crate", "Optional crate id", false)
        );
    }

    @Override
    public DiscordPermissionTier getRequiredTier() {
        return DiscordPermissionTier.ADMIN;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, DiscordCommandContext context) {
        DiscordCommandSupport.deferEphemeral(event);
        String playerName = DiscordCommandSupport.stringOption(event, "player");
        String crateId = DiscordCommandSupport.nullIfBlank(DiscordCommandSupport.stringOption(event, "crate"));

        Bukkit.getScheduler().runTask(context.getPlugin(), () -> {
            Optional<OfflinePlayer> targetOpt = DiscordCommandSupport.findOfflinePlayer(context.getPlugin(), playerName);
            if (targetOpt.isEmpty()) {
                event.getHook().editOriginalEmbeds(context.getEmbeds().error("Player not found.")).queue();
                return;
            }

            OfflinePlayer target = targetOpt.get();
            if (crateId == null) {
                context.getPlugin().getCooldownManager().clearAllCooldowns(target.getUniqueId());
                event.getHook().editOriginalEmbeds(
                    context.getEmbeds().success("Cooldown Cleared", "Cleared all crate cooldowns for **" + target.getName() + "**."))
                    .queue();
            } else {
                if (context.getPlugin().getConfigManager().getCrate(crateId).isEmpty()) {
                    event.getHook().editOriginalEmbeds(context.getEmbeds().error("Unknown crate `" + crateId + "`.")).queue();
                    return;
                }
                context.getPlugin().getCooldownManager().clearCooldown(target.getUniqueId(), crateId);
                event.getHook().editOriginalEmbeds(
                    context.getEmbeds().success("Cooldown Cleared", "Cleared **" + crateId + "** cooldown for **" + target.getName() + "**."))
                    .queue();
            }

            context.getPlugin().getAuditLogger().log(new AuditEntry(
                null,
                event.getUser().getName(),
                AuditEntry.AuditEventType.EDITOR_ACTION,
                target.getUniqueId().toString(),
                "discord cooldown clear " + (crateId == null ? "all" : crateId),
                System.currentTimeMillis()
            ));
        });
    }
}
