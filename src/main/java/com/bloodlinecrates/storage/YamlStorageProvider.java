package com.bloodlinecrates.storage;

import com.bloodlinecrates.model.LeaderboardPeriod;
import com.bloodlinecrates.model.PlayerProfile;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class YamlStorageProvider implements StorageProvider {
    private final JavaPlugin plugin;
    private final File playerDirectory;

    public YamlStorageProvider(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerDirectory = new File(plugin.getDataFolder(), "players");
    }

    @Override
    public void initialize() {
        if (!playerDirectory.exists()) {
            playerDirectory.mkdirs();
        }
    }

    @Override
    public Optional<PlayerProfile> loadProfile(UUID uniqueId) {
        File file = new File(playerDirectory, uniqueId + ".yml");
        if (!file.exists()) {
            return Optional.empty();
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        PlayerProfile profile = new PlayerProfile(uniqueId);
        yaml.getConfigurationSection("virtual_keys").getKeys(false)
                .forEach(key -> profile.virtualKeys().put(key, yaml.getInt("virtual_keys." + key)));
        yaml.getConfigurationSection("pity").getKeys(false)
                .forEach(key -> profile.pityCounters().put(key, yaml.getInt("pity." + key)));
        yaml.getConfigurationSection("crate_cooldowns").getKeys(false)
                .forEach(key -> profile.crateCooldowns().put(key, yaml.getLong("crate_cooldowns." + key)));
        yaml.getConfigurationSection("timed_keys").getKeys(false)
                .forEach(key -> profile.timedKeyNextClaim().put(key, yaml.getLong("timed_keys." + key)));
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            profile.crateOpensByPeriod().put(period, yaml.getInt("leaderboards." + period.name(), 0));
        }
        profile.setGlobalCooldownUntil(yaml.getLong("global_cooldown"));
        profile.setLastReward(yaml.getString("last_reward", ""));
        profile.setLastKnownAddress(yaml.getString("last_known_address", ""));
        profile.setLastOpen(Instant.ofEpochMilli(yaml.getLong("last_open", 0L)));
        for (int i = 0; i < yaml.getLong("total_crates_opened", 0L); i++) {
            profile.incrementTotalCratesOpened();
        }
        return Optional.of(profile);
    }

    @Override
    public void saveProfile(PlayerProfile profile) throws IOException {
        File file = new File(playerDirectory, profile.uniqueId() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        profile.virtualKeys().forEach((key, amount) -> yaml.set("virtual_keys." + key, amount));
        profile.pityCounters().forEach((key, amount) -> yaml.set("pity." + key, amount));
        profile.crateCooldowns().forEach((key, value) -> yaml.set("crate_cooldowns." + key, value));
        profile.timedKeyNextClaim().forEach((key, value) -> yaml.set("timed_keys." + key, value));
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            yaml.set("leaderboards." + period.name(), profile.crateOpensByPeriod().getOrDefault(period, 0));
        }
        yaml.set("global_cooldown", profile.globalCooldownUntil());
        yaml.set("total_crates_opened", profile.totalCratesOpened());
        yaml.set("last_reward", profile.lastReward());
        yaml.set("last_known_address", profile.lastKnownAddress());
        yaml.set("last_open", profile.lastOpen().toEpochMilli());
        yaml.save(file);
    }

    @Override
    public Collection<PlayerProfile> loadAllProfiles() {
        Collection<PlayerProfile> profiles = new ArrayList<>();
        File[] files = playerDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return profiles;
        }
        for (File file : files) {
            try {
                UUID uniqueId = UUID.fromString(file.getName().replace(".yml", ""));
                loadProfile(uniqueId).ifPresent(profiles::add);
            } catch (Exception ignored) {
            }
        }
        return profiles;
    }

    @Override
    public void shutdown() {
    }
}
