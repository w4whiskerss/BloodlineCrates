package com.bloodline.crates.manager;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.model.Reward;
import com.bloodline.crates.pity.PityTracker;
import com.bloodline.crates.stats.PlayerStats;
import com.bloodline.crates.timed.TimedKeyInterval;
import com.bloodline.crates.util.RewardStorageUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    private final BloodlineCrates plugin;
    private final File playerDataFolder;

    public PlayerDataManager(BloodlineCrates plugin) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
    }

    public void setTimedKeyTimestamp(UUID playerId, String crateId, TimedKeyInterval interval, long timestamp) {
        File playerFile = getPlayerFile(playerId);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        String path = "timed-keys." + crateId + "." + interval.name().toLowerCase();
        config.set(path, timestamp);
        save(playerFile, config, "Failed to save timed key timestamp for " + playerId + ": ");
    }

    public long getTimedKeyTimestamp(UUID playerId, String crateId, TimedKeyInterval interval) {
        File playerFile = getPlayerFile(playerId);
        if (!playerFile.exists()) {
            return 0;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        String path = "timed-keys." + crateId + "." + interval.name().toLowerCase();
        return config.getLong(path, 0);
    }

    public void addPendingReward(UUID playerId, Reward reward) {
        createPendingTransaction(playerId, UUID.randomUUID().toString(), reward);
    }

    public List<Reward> getPendingRewards(UUID playerId) {
        List<Reward> rewards = new ArrayList<>();
        for (PendingTransaction transaction : getPendingTransactions(playerId)) {
            rewards.add(transaction.reward());
        }
        return rewards;
    }

    public void clearPendingRewards(UUID playerId) {
        File playerFile = getPlayerFile(playerId);
        if (!playerFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        config.set("pending-transactions", null);
        save(playerFile, config, "Failed to clear pending rewards for " + playerId + ": ");
    }

    public void createPendingTransaction(UUID playerId, String transactionId, Reward reward) {
        File playerFile = getPlayerFile(playerId);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        ConfigurationSection transactionSection = config.createSection("pending-transactions." + transactionId);
        RewardStorageUtil.writeReward(transactionSection, reward);
        transactionSection.set("completed", false);
        save(playerFile, config, "Failed to save pending transaction for " + playerId + ": ");
    }

    public void completePendingTransaction(UUID playerId, String transactionId) {
        File playerFile = getPlayerFile(playerId);
        if (!playerFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        if (!config.contains("pending-transactions." + transactionId)) {
            return;
        }

        config.set("pending-transactions." + transactionId + ".completed", true);
        save(playerFile, config, "Failed to complete pending transaction for " + playerId + ": ");
    }

    public boolean hasPendingTransaction(UUID playerId) {
        return !getPendingTransactions(playerId).isEmpty();
    }

    public List<PendingTransaction> getPendingTransactions(UUID playerId) {
        File playerFile = getPlayerFile(playerId);
        if (!playerFile.exists()) {
            return new ArrayList<>();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        ConfigurationSection pendingSection = config.getConfigurationSection("pending-transactions");
        if (pendingSection == null) {
            return new ArrayList<>();
        }

        List<PendingTransaction> transactions = new ArrayList<>();
        for (String transactionId : pendingSection.getKeys(false)) {
            ConfigurationSection transactionSection = pendingSection.getConfigurationSection(transactionId);
            if (transactionSection == null || transactionSection.getBoolean("completed", false)) {
                continue;
            }

            Reward reward = RewardStorageUtil.readReward(transactionSection);
            if (reward != null) {
                transactions.add(new PendingTransaction(transactionId, reward));
            }
        }
        return transactions;
    }

    public Map<UUID, List<PendingTransaction>> getAllPendingTransactions() {
        Map<UUID, List<PendingTransaction>> pendingTransactions = new HashMap<>();
        File[] playerFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (playerFiles == null) {
            return pendingTransactions;
        }

        for (File playerFile : playerFiles) {
            try {
                UUID playerId = UUID.fromString(playerFile.getName().replace(".yml", ""));
                List<PendingTransaction> transactions = getPendingTransactions(playerId);
                if (!transactions.isEmpty()) {
                    pendingTransactions.put(playerId, transactions);
                }
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Skipping invalid player data file: " + playerFile.getName());
            }
        }

        return pendingTransactions;
    }

    public int getPityCount(UUID playerId, String crateId) {
        File playerFile = getPlayerFile(playerId);
        if (!playerFile.exists()) {
            return 0;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        return config.getInt("pity." + crateId, 0);
    }

    public void setPityCount(UUID playerId, String crateId, int count) {
        File playerFile = getPlayerFile(playerId);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        config.set("pity." + crateId, Math.max(0, count));
        save(playerFile, config, "Failed to save pity count for " + playerId + ": ");
    }

    public List<PityTracker> getPityTrackers(UUID playerId) {
        List<PityTracker> trackers = new ArrayList<>();
        File playerFile = getPlayerFile(playerId);
        if (!playerFile.exists()) {
            return trackers;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        ConfigurationSection section = config.getConfigurationSection("pity");
        if (section == null) {
            return trackers;
        }

        for (String crateId : section.getKeys(false)) {
            trackers.add(new PityTracker(crateId, section.getInt(crateId, 0)));
        }
        return trackers;
    }

    public Map<String, Integer> getVirtualCrates(UUID playerId) {
        File playerFile = getPlayerFile(playerId);
        if (!playerFile.exists()) {
            return new LinkedHashMap<>();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        ConfigurationSection section = config.getConfigurationSection("virtual-crates");
        if (section == null) {
            return new LinkedHashMap<>();
        }

        Map<String, Integer> crates = new LinkedHashMap<>();
        for (String crateId : section.getKeys(false)) {
            int amount = Math.max(0, section.getInt(crateId, 0));
            if (amount > 0) {
                crates.put(crateId, amount);
            }
        }
        return crates;
    }

    public void setVirtualCrates(UUID playerId, Map<String, Integer> virtualCrates) {
        File playerFile = getPlayerFile(playerId);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        config.set("virtual-crates", null);

        Map<String, Integer> safeCrates = virtualCrates == null ? Collections.emptyMap() : virtualCrates;
        for (Map.Entry<String, Integer> entry : safeCrates.entrySet()) {
            int amount = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
            if (amount > 0) {
                config.set("virtual-crates." + entry.getKey(), amount);
            }
        }

        save(playerFile, config, "Failed to save virtual crates for " + playerId + ": ");
    }

    public Map<String, Integer> getVirtualKeys(UUID playerId) {
        File playerFile = getPlayerFile(playerId);
        if (!playerFile.exists()) {
            return new LinkedHashMap<>();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        ConfigurationSection section = config.getConfigurationSection("virtual-keys");
        if (section == null) {
            return new LinkedHashMap<>();
        }

        Map<String, Integer> keys = new LinkedHashMap<>();
        for (String crateId : section.getKeys(false)) {
            int amount = Math.max(0, section.getInt(crateId, 0));
            if (amount > 0) {
                keys.put(crateId, amount);
            }
        }
        return keys;
    }

    public int getCrateOpenCount(UUID playerId, String crateId) {
        File playerFile = getPlayerFile(playerId);
        if (!playerFile.exists()) {
            return 0;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        return Math.max(0, config.getInt("stats.opens-per-crate." + crateId,
            config.getInt("crate-opens." + crateId, 0)));
    }

    public void setCrateOpenCount(UUID playerId, String crateId, int count) {
        File playerFile = getPlayerFile(playerId);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        config.set("stats.opens-per-crate." + crateId, Math.max(0, count));
        config.set("crate-opens." + crateId, null);
        save(playerFile, config, "Failed to save crate open count for " + playerId + ": ");
    }

    public int incrementCrateOpenCount(UUID playerId, String crateId) {
        int updated = getCrateOpenCount(playerId, crateId) + 1;
        setCrateOpenCount(playerId, crateId, updated);
        return updated;
    }

    public void setVirtualKeys(UUID playerId, Map<String, Integer> virtualKeys) {
        File playerFile = getPlayerFile(playerId);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        config.set("virtual-keys", null);

        Map<String, Integer> safeKeys = virtualKeys == null ? Collections.emptyMap() : virtualKeys;
        for (Map.Entry<String, Integer> entry : safeKeys.entrySet()) {
            int amount = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
            if (amount > 0) {
                config.set("virtual-keys." + entry.getKey(), amount);
            }
        }

        save(playerFile, config, "Failed to save virtual keys for " + playerId + ": ");
    }

    public PlayerStats loadStats(UUID playerId) {
        File playerFile = getPlayerFile(playerId);
        PlayerStats stats = new PlayerStats(playerId);
        if (!playerFile.exists()) {
            return stats;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        stats.setPlayerName(config.getString("stats.player-name", ""));
        stats.setOpensPerCrate(readIntMap(config.getConfigurationSection("stats.opens-per-crate")));
        if (stats.getOpensPerCrate().isEmpty()) {
            stats.setOpensPerCrate(readIntMap(config.getConfigurationSection("crate-opens")));
        }
        stats.setRarestRewardPerCrate(readStringMap(config.getConfigurationSection("stats.rarest-reward-per-crate")));
        stats.setRarestChancePerCrate(readDoubleMap(config.getConfigurationSection("stats.rarest-chance-per-crate")));
        stats.setCurrentPityPerCrate(readIntMap(config.getConfigurationSection("pity")));
        stats.setLastOpenedPerCrate(readLongMap(config.getConfigurationSection("stats.last-opened-per-crate")));
        stats.setTotalOpensAllTime(config.getInt("stats.total-opens-all-time", stats.getOpensPerCrate().values().stream().mapToInt(Integer::intValue).sum()));
        stats.setFirstOpenTimestamp(config.getLong("stats.first-open-timestamp", 0L));
        return stats;
    }

    public void saveStats(PlayerStats stats) {
        File playerFile = getPlayerFile(stats.getPlayerUUID());
        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        config.set("stats.player-name", stats.getPlayerName());
        writeMap(config, "stats.opens-per-crate", stats.getOpensPerCrate());
        writeMap(config, "stats.rarest-reward-per-crate", stats.getRarestRewardPerCrate());
        writeMap(config, "stats.rarest-chance-per-crate", stats.getRarestChancePerCrate());
        writeMap(config, "stats.last-opened-per-crate", stats.getLastOpenedPerCrate());
        config.set("stats.total-opens-all-time", Math.max(0, stats.getTotalOpensAllTime()));
        config.set("stats.first-open-timestamp", Math.max(0L, stats.getFirstOpenTimestamp()));
        config.set("crate-opens", null);

        save(playerFile, config, "Failed to save stats for " + stats.getPlayerUUID() + ": ");
    }

    public List<UUID> getAllKnownPlayerIds() {
        List<UUID> result = new ArrayList<>();
        File[] files = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return result;
        }

        for (File file : files) {
            try {
                result.add(UUID.fromString(file.getName().replace(".yml", "")));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    private File getPlayerFile(UUID playerId) {
        return new File(playerDataFolder, playerId + ".yml");
    }

    private Map<String, Integer> readIntMap(ConfigurationSection section) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            result.put(key, Math.max(0, section.getInt(key, 0)));
        }
        return result;
    }

    private Map<String, String> readStringMap(ConfigurationSection section) {
        Map<String, String> result = new LinkedHashMap<>();
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            result.put(key, section.getString(key, ""));
        }
        return result;
    }

    private Map<String, Double> readDoubleMap(ConfigurationSection section) {
        Map<String, Double> result = new LinkedHashMap<>();
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            result.put(key, section.getDouble(key, 0.0D));
        }
        return result;
    }

    private Map<String, Long> readLongMap(ConfigurationSection section) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            result.put(key, Math.max(0L, section.getLong(key, 0L)));
        }
        return result;
    }

    private void writeMap(YamlConfiguration config, String path, Map<String, ?> values) {
        config.set(path, null);
        if (values == null) {
            return;
        }
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            config.set(path + "." + entry.getKey(), entry.getValue());
        }
    }

    private void save(File playerFile, YamlConfiguration config, String errorPrefix) {
        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe(errorPrefix + e.getMessage());
        }
    }

    public record PendingTransaction(String transactionId, Reward reward) {
    }
}
