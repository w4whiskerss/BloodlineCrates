package com.bloodline.crates.security;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class IPTracker {
    private final BloodlineCrates plugin;
    private final File dataFile;
    private final Map<UUID, String> uuidToIp = new HashMap<>();
    private final Map<String, Set<UUID>> ipToUuids = new HashMap<>();

    public IPTracker(BloodlineCrates plugin) {
        this.plugin = plugin;
        File securityFolder = new File(plugin.getDataFolder(), "security");
        if (!securityFolder.exists()) {
            securityFolder.mkdirs();
        }
        this.dataFile = new File(securityFolder, "ip_data.yml");
        load();
    }

    public void recordPlayer(UUID playerId, String ipAddress) {
        String storedIp = normalizeIp(ipAddress);
        String previous = uuidToIp.put(playerId, storedIp);

        if (previous != null && !previous.equals(storedIp)) {
            Set<UUID> previousSet = ipToUuids.get(previous);
            if (previousSet != null) {
                previousSet.remove(playerId);
                if (previousSet.isEmpty()) {
                    ipToUuids.remove(previous);
                }
            }
        }

        ipToUuids.computeIfAbsent(storedIp, ignored -> new HashSet<>()).add(playerId);
        save();
    }

    public int getAccountCount(UUID playerId) {
        String storedIp = uuidToIp.get(playerId);
        if (storedIp == null) {
            return 0;
        }

        return ipToUuids.getOrDefault(storedIp, Collections.emptySet()).size();
    }

    public Set<UUID> getAccountsFor(UUID playerId) {
        String storedIp = uuidToIp.get(playerId);
        if (storedIp == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(ipToUuids.getOrDefault(storedIp, Collections.emptySet()));
    }

    private String normalizeIp(String ipAddress) {
        if (plugin.getConfig().getBoolean("security.privacy-mode", false)) {
            return hashIp(ipAddress);
        }

        return ipAddress;
    }

    private String hashIp(String ipAddress) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(ipAddress.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : hashed) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            plugin.getLogger().warning("SHA-256 unavailable for privacy-mode IP hashing, falling back to raw IP storage.");
            return ipAddress;
        }
    }

    private void load() {
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection playersSection = configuration.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }

        for (String key : playersSection.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                String storedIp = playersSection.getString(key);
                if (storedIp == null || storedIp.isEmpty()) {
                    continue;
                }

                uuidToIp.put(playerId, storedIp);
                ipToUuids.computeIfAbsent(storedIp, ignored -> new HashSet<>()).add(playerId);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Skipping malformed UUID in ip_data.yml: " + key);
            }
        }
    }

    private void save() {
        YamlConfiguration configuration = new YamlConfiguration();
        for (Map.Entry<UUID, String> entry : uuidToIp.entrySet()) {
            configuration.set("players." + entry.getKey(), entry.getValue());
        }

        try {
            configuration.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save IP tracker data: " + exception.getMessage());
        }
    }
}
