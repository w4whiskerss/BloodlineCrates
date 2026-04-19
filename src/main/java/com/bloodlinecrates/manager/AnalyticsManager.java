package com.bloodlinecrates.manager;

import com.bloodlinecrates.model.CrateDefinition;
import com.bloodlinecrates.model.CrateReward;
import com.bloodlinecrates.model.LeaderboardPeriod;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AnalyticsManager {
    private final JavaPlugin plugin;
    private final Map<String, Integer> totalOpensByCrate = new HashMap<>();
    private final Map<LeaderboardPeriod, Map<UUID, Integer>> leaderboards = new EnumMap<>(LeaderboardPeriod.class);

    public AnalyticsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            leaderboards.put(period, new HashMap<>());
        }
    }

    public void recordOpen(Player player, CrateDefinition crate, int amount) {
        totalOpensByCrate.merge(crate.id(), amount, Integer::sum);
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            leaderboards.get(period).merge(player.getUniqueId(), amount, Integer::sum);
        }
        append("opens.log", LocalDate.now() + "," + player.getUniqueId() + "," + crate.id() + "," + amount);
    }

    public void recordReward(Player player, CrateDefinition crate, CrateReward reward) {
        append("rewards.log", LocalDate.now() + "," + player.getUniqueId() + "," + crate.id() + "," + reward.id());
    }

    public String topPlayerName(LeaderboardPeriod period, int position) {
        return leaderboards.get(period).entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getValue(), left.getValue()))
                .skip(position - 1L)
                .findFirst()
                .map(entry -> plugin.getServer().getOfflinePlayer(entry.getKey()).getName())
                .orElse("None");
    }

    public String mostOpenedCrate() {
        return totalOpensByCrate.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("none");
    }

    private void append(String fileName, String line) {
        File file = new File(plugin.getDataFolder(), fileName);
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(line + System.lineSeparator());
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to append analytics log: " + exception.getMessage());
        }
    }
}
