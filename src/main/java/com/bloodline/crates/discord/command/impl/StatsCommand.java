package com.bloodline.crates.discord.command.impl;

import com.bloodline.crates.discord.DiscordCommandContext;
import com.bloodline.crates.discord.DiscordPermissionTier;
import com.bloodline.crates.discord.command.DiscordCommand;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.stats.PlayerStats;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StatsCommand implements DiscordCommand {
    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public String getDescription() {
        return "View player crate stats";
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
            PlayerStats stats = context.getPlugin().getStatsManager().getStats(target.getUniqueId());
            List<String> lines = new ArrayList<>();
            for (String crateId : stats.getOpensPerCrate().keySet()) {
                Crate crate = context.getPlugin().getConfigManager().getCrate(crateId).orElse(null);
                String crateName = crate == null ? crateId : crate.getDisplayName();
                lines.add(crateName + " - " + stats.getOpensPerCrate().getOrDefault(crateId, 0)
                    + " opens, rarest " + stats.getRarestRewardPerCrate().getOrDefault(crateId, "None"));
            }
            if (lines.isEmpty()) {
                lines.add("No crate stats recorded.");
            }

            String displayName = target.getName() != null ? target.getName() : stats.getPlayerName();
            var embed = context.getEmbeds().base("BloodlineCrates - Stats", "Stats for **" + displayName + "**", com.bloodline.crates.discord.DiscordEmbedBuilder.COLOUR_INFO)
                .addField("Total Opens", String.valueOf(stats.getTotalOpensAllTime()), true)
                .addField("First Open", context.getPlugin().getStatsManager().formatDate(stats.getFirstOpenTimestamp()), true)
                .addField("Per Crate", String.join("\n", lines), false)
                .build();
            event.getHook().editOriginalEmbeds(embed).queue();
        });
    }
}
