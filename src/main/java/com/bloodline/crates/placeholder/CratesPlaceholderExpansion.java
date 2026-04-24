package com.bloodline.crates.placeholder;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.leaderboard.LeaderboardEntry;
import com.bloodline.crates.leaderboard.LeaderboardPeriod;
import com.bloodline.crates.model.Crate;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CratesPlaceholderExpansion extends PlaceholderExpansion {
    private final BloodlineCrates plugin;

    public CratesPlaceholderExpansion(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "bloodcrates";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.startsWith("cooldown_")) {
            return handleCooldownPlaceholder(player, params);
        }

        if (params.startsWith("keys_")) {
            return handleKeysPlaceholder(player, params);
        }

        if (params.equalsIgnoreCase("keys_total")) {
            return handleTotalKeysPlaceholder(player);
        }

        if (params.startsWith("haskey_")) {
            return handleHasKeyPlaceholder(player, params);
        }

        if (params.startsWith("timedkey_")) {
            return handleTimedKeyPlaceholder(player, params);
        }

        if (params.startsWith("opens_")) {
            return handleOpensPlaceholder(player, params);
        }

        if (params.startsWith("rank_")) {
            return handleRankPlaceholder(player, params);
        }

        if (params.startsWith("top_")) {
            return handleTopPlaceholder(params);
        }

        if (params.startsWith("timeat1_")) {
            return handleTimeAt1Placeholder(player, params);
        }

        if (params.equalsIgnoreCase("stats_total")) {
            return handleStatsTotal(player);
        }

        if (params.startsWith("stats_opens_")) {
            return handleStatsOpens(player, params.substring("stats_opens_".length()));
        }

        if (params.startsWith("stats_rarest_chance_")) {
            return handleStatsRarestChance(player, params.substring("stats_rarest_chance_".length()));
        }

        if (params.startsWith("stats_rarest_")) {
            return handleStatsRarest(player, params.substring("stats_rarest_".length()));
        }

        if (params.startsWith("stats_last_")) {
            return handleStatsLast(player, params.substring("stats_last_".length()));
        }

        return null;
    }

    private String handleStatsTotal(OfflinePlayer player) {
        if (player == null) {
            return "0";
        }
        return String.valueOf(plugin.getStatsManager().getStats(player.getUniqueId()).getTotalOpensAllTime());
    }

    private String handleStatsOpens(OfflinePlayer player, String crateId) {
        if (player == null || crateId == null || crateId.isBlank()) {
            return "0";
        }
        return String.valueOf(plugin.getStatsManager().getStats(player.getUniqueId()).getOpensPerCrate().getOrDefault(crateId.toLowerCase(Locale.ROOT), 0));
    }

    private String handleStatsRarest(OfflinePlayer player, String crateId) {
        if (player == null || crateId == null || crateId.isBlank()) {
            return "None";
        }
        return plugin.getStatsManager().getStats(player.getUniqueId()).getRarestRewardPerCrate().getOrDefault(crateId.toLowerCase(Locale.ROOT), "None");
    }

    private String handleStatsRarestChance(OfflinePlayer player, String crateId) {
        if (player == null || crateId == null || crateId.isBlank()) {
            return "0.00%";
        }
        return plugin.getStatsManager().formatChance(
            plugin.getStatsManager().getStats(player.getUniqueId()).getRarestChancePerCrate().get(crateId.toLowerCase(Locale.ROOT)));
    }

    private String handleStatsLast(OfflinePlayer player, String crateId) {
        if (player == null || crateId == null || crateId.isBlank()) {
            return "Never";
        }
        long timestamp = plugin.getStatsManager().getStats(player.getUniqueId()).getLastOpenedPerCrate().getOrDefault(crateId.toLowerCase(Locale.ROOT), 0L);
        return plugin.getStatsManager().formatRelative(timestamp);
    }

    private String handleKeysPlaceholder(OfflinePlayer player, String params) {
        if (player == null || !player.isOnline()) {
            return "0";
        }

        String crateId = params.substring("keys_".length());
        if (crateId.isBlank()) {
            return "0";
        }

        Crate crate = plugin.getConfigManager().getCrate(crateId).orElse(null);
        if (crate == null) {
            return "0";
        }

        return String.valueOf(plugin.getKeyManager().getKeyCount(player.getPlayer(), crate));
    }

    private String handleTotalKeysPlaceholder(OfflinePlayer player) {
        if (player == null || !player.isOnline()) {
            return "0";
        }

        int total = plugin.getConfigManager().getAllCrates().stream()
            .mapToInt(crate -> plugin.getKeyManager().getKeyCount(player.getPlayer(), crate))
            .sum();
        return String.valueOf(total);
    }

    private String handleHasKeyPlaceholder(OfflinePlayer player, String params) {
        if (player == null || !player.isOnline()) {
            return "false";
        }

        String crateId = params.substring("haskey_".length());
        if (crateId.isBlank()) {
            return "false";
        }

        Crate crate = plugin.getConfigManager().getCrate(crateId).orElse(null);
        if (crate == null) {
            return "false";
        }

        return String.valueOf(plugin.getKeyManager().hasKey(player.getPlayer(), crate));
    }

    private String handleCooldownPlaceholder(OfflinePlayer player, String params) {
        if (player == null) {
            return params.endsWith("_raw") ? "0" : "Ready";
        }

        String suffix = params.substring("cooldown_".length());
        boolean raw = suffix.endsWith("_raw");
        String crateId = raw ? suffix.substring(0, suffix.length() - 4) : suffix;
        if (crateId.isBlank()) {
            return raw ? "0" : "Ready";
        }

        var crate = plugin.getConfigManager().getCrate(crateId).orElse(null);
        if (crate == null) {
            return raw ? "0" : "Ready";
        }

        long remainingMillis = plugin.getCooldownManager().getRemainingMillis(player.getUniqueId(), crate);
        return raw ? String.valueOf(remainingMillis) : plugin.getCooldownManager().formatRemaining(remainingMillis);
    }

    private String handleTimedKeyPlaceholder(OfflinePlayer player, String params) {
        if (player == null || !player.isOnline()) {
            return "N/A";
        }

        String[] parts = params.split("_");
        if (parts.length < 3) {
            return "Invalid";
        }

        String crateId = parts[1];
        if (!parts[2].equals("countdown")) {
            return "Invalid";
        }

        long millisRemaining = plugin.getTimedKeyManager().getTimeUntilNext(player.getPlayer(), crateId);
        return formatTime(millisRemaining);
    }

    private String handleOpensPlaceholder(OfflinePlayer player, String params) {
        if (player == null || !plugin.getLeaderboardManager().isEnabled()) {
            return "0";
        }

        String periodStr = params.substring(6);
        LeaderboardPeriod period = parsePeriod(periodStr);
        if (period == null) {
            return "Invalid";
        }

        int opens = plugin.getLeaderboardManager().getPlayerOpens(player.getUniqueId(), period);
        return String.valueOf(opens);
    }

    private String handleRankPlaceholder(OfflinePlayer player, String params) {
        if (player == null || !plugin.getLeaderboardManager().isEnabled()) {
            return "0";
        }

        String periodStr = params.substring(5);
        LeaderboardPeriod period = parsePeriod(periodStr);
        if (period == null) {
            return "Invalid";
        }

        int rank = plugin.getLeaderboardManager().getPlayerRank(player.getUniqueId(), period);
        return String.valueOf(rank);
    }

    private String handleTopPlaceholder(String params) {
        if (!plugin.getLeaderboardManager().isEnabled()) {
            return "N/A";
        }

        String[] parts = params.split("_");
        if (parts.length < 4) {
            return "Invalid";
        }

        LeaderboardPeriod period = parsePeriod(parts[1]);
        if (period == null) {
            return "Invalid";
        }

        int rank;
        try {
            rank = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return "Invalid";
        }

        String type = parts[3];

        List<LeaderboardEntry> leaderboard = plugin.getLeaderboardManager().getLeaderboard(period, rank);
        if (leaderboard.size() < rank) {
            return "N/A";
        }

        LeaderboardEntry entry = leaderboard.get(rank - 1);

        if (type.equals("name")) {
            return entry.getPlayerName();
        } else if (type.equals("opens")) {
            return String.valueOf(entry.getCrateOpens());
        }

        return "Invalid";
    }

    private String handleTimeAt1Placeholder(OfflinePlayer player, String params) {
        if (player == null || !plugin.getLeaderboardManager().isEnabled()) {
            return "0s";
        }

        String periodStr = params.substring(8);
        LeaderboardPeriod period = parsePeriod(periodStr);
        if (period == null) {
            return "Invalid";
        }

        long millis = plugin.getLeaderboardManager().getPlayerTimeAtTop(player.getUniqueId(), period);
        return formatTime(millis);
    }

    private LeaderboardPeriod parsePeriod(String periodStr) {
        try {
            return LeaderboardPeriod.valueOf(periodStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String formatTime(long millis) {
        if (millis <= 0) {
            return "00:00:00";
        }

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
