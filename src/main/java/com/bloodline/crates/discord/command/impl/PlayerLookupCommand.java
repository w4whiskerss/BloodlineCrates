package com.bloodline.crates.discord.command.impl;

import com.bloodline.crates.discord.DiscordCommandContext;
import com.bloodline.crates.discord.DiscordPermissionTier;
import com.bloodline.crates.discord.command.DiscordCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlayerLookupCommand implements DiscordCommand {
    @Override
    public String getName() {
        return "lookup";
    }

    @Override
    public String getDescription() {
        return "Inspect a player's crate-related state";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(new OptionData(OptionType.STRING, "player", "Player name", true));
    }

    @Override
    public DiscordPermissionTier getRequiredTier() {
        return DiscordPermissionTier.MODERATOR;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, DiscordCommandContext context) {
        DiscordCommandSupport.deferEphemeral(event);
        String name = DiscordCommandSupport.stringOption(event, "player");
        Bukkit.getScheduler().runTask(context.getPlugin(), () -> {
            Optional<OfflinePlayer> targetOpt = DiscordCommandSupport.findOfflinePlayer(context.getPlugin(), name);
            if (targetOpt.isEmpty()) {
                event.getHook().editOriginalEmbeds(context.getEmbeds().error("Player not found.")).queue();
                return;
            }
            OfflinePlayer target = targetOpt.get();
            Player online = target.getPlayer();
            String status = online != null && online.isOnline() ? "Online" : "Offline";

            List<String> cooldowns = new ArrayList<>();
            if (online != null) {
                context.getPlugin().getConfigManager().getAllCrates().forEach(crate -> {
                    long remaining = context.getPlugin().getCooldownManager().getRemainingMillis(online.getUniqueId(), crate);
                    if (remaining > 0L) {
                        cooldowns.add(crate.getId() + ": " + context.getPlugin().getCooldownManager().formatRemaining(remaining));
                    }
                });
            }
            if (cooldowns.isEmpty()) {
                cooldowns.add("None");
            }

            String displayName = target.getName() == null ? target.getUniqueId().toString() : target.getName();
            var embed = context.getEmbeds().base("BloodlineCrates - Player Lookup", "Lookup for **" + displayName + "**", com.bloodline.crates.discord.DiscordEmbedBuilder.COLOUR_INFO)
                .addField("Status", status, true)
                .addField("Virtual Crates", context.getPlugin().getVirtualInventoryManager().getAllVirtualCrates(target.getUniqueId()).toString(), false)
                .addField("Pity Counters", context.getPlugin().getStatsManager().getStats(target.getUniqueId()).getCurrentPityPerCrate().toString(), false)
                .addField("Pending Claims", String.valueOf(context.getPlugin().getClaimsManager().getClaims(target.getUniqueId()).size()), true)
                .addField("Cooldowns", String.join("\n", cooldowns), false)
                .build();
            event.getHook().editOriginalEmbeds(embed).queue();
        });
    }
}
