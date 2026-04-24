package com.bloodline.crates.stats;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.Reward;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class StatsManager {
    private final BloodlineCrates plugin;
    private final Map<UUID, PlayerStats> cache = new LinkedHashMap<>();
    private final DecimalFormat chanceFormat = new DecimalFormat("0.00");

    public StatsManager(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    public PlayerStats getStats(UUID playerUUID) {
        return cache.computeIfAbsent(playerUUID, plugin.getPlayerDataManager()::loadStats);
    }

    public void recordOpen(UUID playerUUID, String playerName, Crate crate, Reward reward) {
        PlayerStats stats = getStats(playerUUID);
        String crateId = normalize(crate.getId());
        long now = System.currentTimeMillis();

        stats.setPlayerName(playerName);
        stats.getOpensPerCrate().put(crateId, plugin.getPlayerDataManager().getCrateOpenCount(playerUUID, crateId));
        stats.getCurrentPityPerCrate().put(crateId, plugin.getPlayerDataManager().getPityCount(playerUUID, crateId));
        stats.getLastOpenedPerCrate().put(crateId, now);
        stats.setTotalOpensAllTime(Math.max(stats.getTotalOpensAllTime() + 1, totalOpens(stats.getOpensPerCrate())));
        if (stats.getFirstOpenTimestamp() <= 0L) {
            stats.setFirstOpenTimestamp(now);
        }

        Double currentRarestChance = stats.getRarestChancePerCrate().get(crateId);
        if (currentRarestChance == null || reward.getChance() < currentRarestChance) {
            stats.getRarestChancePerCrate().put(crateId, reward.getChance());
            stats.getRarestRewardPerCrate().put(crateId, reward.getDisplayNameOrFallback());
        }

        plugin.getPlayerDataManager().saveStats(stats);
    }

    public List<PlayerStats> getTopPlayers(String crateId, int topN) {
        String normalized = normalize(crateId);
        List<PlayerStats> stats = new ArrayList<>();
        for (UUID playerUUID : plugin.getPlayerDataManager().getAllKnownPlayerIds()) {
            stats.add(getStats(playerUUID));
        }

        return stats.stream()
            .sorted(Comparator
                .comparingInt((PlayerStats entry) -> entry.getOpensPerCrate().getOrDefault(normalized, 0))
                .reversed()
                .thenComparing(PlayerStats::getPlayerName, String.CASE_INSENSITIVE_ORDER))
            .limit(topN)
            .toList();
    }

    public String formatChance(Double chance) {
        return chance == null ? "N/A" : chanceFormat.format(chance) + "%";
    }

    public String formatDate(long timestamp) {
        if (timestamp <= 0L) {
            return "Never";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date(timestamp));
    }

    public String formatRelative(long timestamp) {
        if (timestamp <= 0L) {
            return "Never";
        }

        long diff = Math.max(0L, System.currentTimeMillis() - timestamp);
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        if (days > 0L) {
            return days + "d ago";
        }
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        if (hours > 0L) {
            return hours + "h ago";
        }
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        if (minutes > 0L) {
            return minutes + "m ago";
        }
        long seconds = Math.max(1L, TimeUnit.MILLISECONDS.toSeconds(diff));
        return seconds + "s ago";
    }

    public String getOverallRarestReward(PlayerStats stats) {
        String bestReward = "None";
        double bestChance = Double.MAX_VALUE;
        for (Map.Entry<String, Double> entry : stats.getRarestChancePerCrate().entrySet()) {
            double chance = entry.getValue() == null ? Double.MAX_VALUE : entry.getValue();
            if (chance < bestChance) {
                bestChance = chance;
                bestReward = stats.getRarestRewardPerCrate().getOrDefault(entry.getKey(), "None");
            }
        }
        return bestChance == Double.MAX_VALUE ? "None" : bestReward + " (" + formatChance(bestChance) + ")";
    }

    public int getServerOpens(String crateId) {
        return plugin.getBroadcastManager().getServerOpens(crateId);
    }

    private int totalOpens(Map<String, Integer> opensPerCrate) {
        return opensPerCrate.values().stream().mapToInt(Integer::intValue).sum();
    }

    private String normalize(String crateId) {
        return crateId == null ? "" : crateId.toLowerCase(Locale.ROOT);
    }
}
