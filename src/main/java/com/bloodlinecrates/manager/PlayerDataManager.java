package com.bloodlinecrates.manager;

import com.bloodlinecrates.model.PlayerProfile;
import com.bloodlinecrates.storage.StorageProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDataManager {
    private final JavaPlugin plugin;
    private final StorageProvider storageProvider;
    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();

    public PlayerDataManager(JavaPlugin plugin, StorageProvider storageProvider) {
        this.plugin = plugin;
        this.storageProvider = storageProvider;
    }

    public void loadAll() throws Exception {
        for (PlayerProfile profile : storageProvider.loadAllProfiles()) {
            profiles.put(profile.uniqueId(), profile);
        }
    }

    public PlayerProfile getOrLoad(UUID uniqueId) {
        return profiles.computeIfAbsent(uniqueId, id -> {
            try {
                return storageProvider.loadProfile(id).orElse(new PlayerProfile(id));
            } catch (Exception exception) {
                plugin.getLogger().warning("Failed to load profile for " + id + ": " + exception.getMessage());
                return new PlayerProfile(id);
            }
        });
    }

    public void save(PlayerProfile profile) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                storageProvider.saveProfile(profile);
            } catch (Exception exception) {
                plugin.getLogger().warning("Failed to save profile for " + profile.uniqueId() + ": " + exception.getMessage());
            }
        });
    }

    public void saveAll() {
        profiles.values().forEach(this::save);
    }

    public Collection<PlayerProfile> allProfiles() {
        return profiles.values();
    }
}
