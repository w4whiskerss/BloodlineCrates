package com.bloodline.crates.discord;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.audit.AuditEntry;
import com.bloodline.crates.leaderboard.LeaderboardEntry;
import com.bloodline.crates.leaderboard.LeaderboardPeriod;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.Reward;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscordAlertManager {
    private final BloodlineCrates plugin;
    private final DiscordBotManager botManager;
    private final DiscordEmbedBuilder embeds;
    private final Map<String, Integer> lastServerOpenSnapshot = new HashMap<>();
    private volatile String mostRecentRareDrop = "None";

    public DiscordAlertManager(BloodlineCrates plugin, DiscordBotManager botManager, DiscordEmbedBuilder embeds) {
        this.plugin = plugin;
        this.botManager = botManager;
        this.embeds = embeds;
    }

    public void alertRareReward(Player player, Crate crate, Reward reward) {
        mostRecentRareDrop = player.getName() + " - " + reward.getDisplayNameOrFallback() + " from " + crate.getDisplayName();
        sendAlert(embeds.rare("Rare Reward",
            player.getName() + " won **" + reward.getDisplayNameOrFallback() + "** from **" + crate.getDisplayName() + "** (" + reward.getChance() + "%)."));
    }

    public void alertJackpot(Player player, Crate crate, Reward reward) {
        sendAlert(embeds.alert("Jackpot", player.getName() + " hit a jackpot on **" + crate.getDisplayName() + "** and won **" + reward.getDisplayNameOrFallback() + "**."));
    }

    public void alertAntiAlt(Player flagged, Player existingAccount) {
        String description = existingAccount == null
            ? flagged.getName() + " triggered the anti-alt system."
            : flagged.getName() + " appears linked to " + existingAccount.getName() + ".";
        sendAlert(embeds.alert("Anti-Alt Flag", description));
    }

    public void alertGlobalLimitReached(Crate crate) {
        sendAlert(embeds.alert("Global Limit Reached", "**" + crate.getDisplayName() + "** has reached its global open limit."));
    }

    public void alertCrateEvent(String eventName, boolean started) {
        sendAlert(embeds.info("Crate Event", "Event **" + eventName + "** has " + (started ? "started" : "stopped") + "."));
    }

    public void postScheduledStats() {
        if (!botManager.isConnected() || botManager.getDiscordConfig().getStatsChannelId() == null) {
            return;
        }

        List<LeaderboardEntry> topPlayers = plugin.getLeaderboardManager().getLeaderboard(LeaderboardPeriod.DAILY, 5);
        StringBuilder leaderboard = new StringBuilder();
        for (int i = 0; i < topPlayers.size(); i++) {
            LeaderboardEntry entry = topPlayers.get(i);
            leaderboard.append(i + 1).append(". ").append(entry.getPlayerName()).append(" - ").append(entry.getCrateOpens()).append(" opens");
            if (i < topPlayers.size() - 1) {
                leaderboard.append('\n');
            }
        }
        if (leaderboard.isEmpty()) {
            leaderboard.append("No daily data yet.");
        }

        int totalIntervalOpens = 0;
        for (Crate crate : plugin.getConfigManager().getAllCrates()) {
            int current = plugin.getBroadcastManager().getServerOpens(crate.getId());
            int previous = lastServerOpenSnapshot.getOrDefault(crate.getId().toLowerCase(), 0);
            totalIntervalOpens += Math.max(0, current - previous);
            lastServerOpenSnapshot.put(crate.getId().toLowerCase(), current);
        }

        Crate popular = plugin.getConfigManager().getAllCrates().stream()
            .max(Comparator.comparingInt(crate -> plugin.getBroadcastManager().getServerOpens(crate.getId())))
            .orElse(null);

        MessageEmbed embed = embeds.base("BloodlineCrates - Scheduled Stats", "Automated crate summary", DiscordEmbedBuilder.COLOUR_INFO)
            .addField("Top 5 Today", leaderboard.toString(), false)
            .addField("Opens In Last Interval", String.valueOf(totalIntervalOpens), true)
            .addField("Most Popular Crate", popular == null ? "None" : popular.getDisplayName(), true)
            .addField("Most Recent Rare Drop", mostRecentRareDrop, false)
            .build();
        sendToChannel(botManager.getDiscordConfig().getStatsChannelId(), embed);
    }

    public void mirrorAuditEntry(AuditEntry entry) {
        if (!botManager.isConnected() || botManager.getDiscordConfig().getAuditLogChannelId() == null) {
            return;
        }
        MessageEmbed embed = embeds.base("BloodlineCrates - Audit", entry.getDetail(), DiscordEmbedBuilder.COLOUR_INFO)
            .addField("Type", entry.getEventType().name(), true)
            .addField("Actor", entry.getActorName() == null ? "Console" : entry.getActorName(), true)
            .addField("Target", entry.getTargetId() == null ? "-" : entry.getTargetId(), true)
            .build();
        sendToChannel(botManager.getDiscordConfig().getAuditLogChannelId(), embed);
    }

    private void sendAlert(MessageEmbed embed) {
        sendToChannel(botManager.getDiscordConfig().getAlertChannelId(), embed);
    }

    private void sendToChannel(String channelId, MessageEmbed embed) {
        if (channelId == null || !botManager.isConnected()) {
            return;
        }
        TextChannel channel = botManager.getGuild() == null ? null : botManager.getGuild().getTextChannelById(channelId);
        if (channel == null) {
            return;
        }
        channel.sendMessageEmbeds(embed).queue();
    }
}
