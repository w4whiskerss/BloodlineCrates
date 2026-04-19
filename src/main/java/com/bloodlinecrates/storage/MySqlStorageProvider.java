package com.bloodlinecrates.storage;

import com.bloodlinecrates.model.LeaderboardPeriod;
import com.bloodlinecrates.model.PlayerProfile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class MySqlStorageProvider implements StorageProvider {
    private final JavaPlugin plugin;
    private final ConfigurationSection config;
    private Connection connection;

    public MySqlStorageProvider(JavaPlugin plugin, ConfigurationSection config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public void initialize() throws Exception {
        String jdbc = "jdbc:mysql://" + config.getString("host") + ":" + config.getInt("port") + "/" + config.getString("database")
                + "?useSSL=" + config.getBoolean("use_ssl", false) + "&allowPublicKeyRetrieval=true";
        this.connection = DriverManager.getConnection(jdbc, config.getString("username"), config.getString("password"));
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS bloodline_profiles (
                        uuid VARCHAR(36) PRIMARY KEY,
                        global_cooldown BIGINT NOT NULL,
                        total_crates_opened BIGINT NOT NULL,
                        last_reward VARCHAR(128) NOT NULL,
                        last_known_address VARCHAR(64) NOT NULL,
                        last_open BIGINT NOT NULL,
                        virtual_keys TEXT NOT NULL,
                        pity TEXT NOT NULL,
                        crate_cooldowns TEXT NOT NULL,
                        timed_keys TEXT NOT NULL,
                        leaderboards TEXT NOT NULL
                    )
                    """);
        }
    }

    @Override
    public Optional<PlayerProfile> loadProfile(UUID uniqueId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM bloodline_profiles WHERE uuid = ?")) {
            statement.setString(1, uniqueId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                PlayerProfile profile = new PlayerProfile(uniqueId);
                profile.setGlobalCooldownUntil(resultSet.getLong("global_cooldown"));
                profile.setLastReward(resultSet.getString("last_reward"));
                profile.setLastKnownAddress(resultSet.getString("last_known_address"));
                profile.setLastOpen(Instant.ofEpochMilli(resultSet.getLong("last_open")));
                for (int i = 0; i < resultSet.getLong("total_crates_opened"); i++) {
                    profile.incrementTotalCratesOpened();
                }
                deserializeMap(resultSet.getString("virtual_keys")).forEach((key, value) -> profile.virtualKeys().put(key, Integer.parseInt(value)));
                deserializeMap(resultSet.getString("pity")).forEach((key, value) -> profile.pityCounters().put(key, Integer.parseInt(value)));
                deserializeMap(resultSet.getString("crate_cooldowns")).forEach((key, value) -> profile.crateCooldowns().put(key, Long.parseLong(value)));
                deserializeMap(resultSet.getString("timed_keys")).forEach((key, value) -> profile.timedKeyNextClaim().put(key, Long.parseLong(value)));
                deserializeMap(resultSet.getString("leaderboards")).forEach((key, value) -> profile.crateOpensByPeriod().put(LeaderboardPeriod.valueOf(key), Integer.parseInt(value)));
                return Optional.of(profile);
            }
        }
    }

    @Override
    public void saveProfile(PlayerProfile profile) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO bloodline_profiles (uuid, global_cooldown, total_crates_opened, last_reward, last_known_address, last_open, virtual_keys, pity, crate_cooldowns, timed_keys, leaderboards)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                global_cooldown = VALUES(global_cooldown),
                total_crates_opened = VALUES(total_crates_opened),
                last_reward = VALUES(last_reward),
                last_known_address = VALUES(last_known_address),
                last_open = VALUES(last_open),
                virtual_keys = VALUES(virtual_keys),
                pity = VALUES(pity),
                crate_cooldowns = VALUES(crate_cooldowns),
                timed_keys = VALUES(timed_keys),
                leaderboards = VALUES(leaderboards)
                """)) {
            statement.setString(1, profile.uniqueId().toString());
            statement.setLong(2, profile.globalCooldownUntil());
            statement.setLong(3, profile.totalCratesOpened());
            statement.setString(4, profile.lastReward());
            statement.setString(5, profile.lastKnownAddress());
            statement.setLong(6, profile.lastOpen().toEpochMilli());
            statement.setString(7, serializeMap(profile.virtualKeys()));
            statement.setString(8, serializeMap(profile.pityCounters()));
            statement.setString(9, serializeMap(profile.crateCooldowns()));
            statement.setString(10, serializeMap(profile.timedKeyNextClaim()));
            statement.setString(11, serializeMap(profile.crateOpensByPeriod()));
            statement.executeUpdate();
        }
    }

    @Override
    public Collection<PlayerProfile> loadAllProfiles() throws Exception {
        Collection<PlayerProfile> profiles = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT uuid FROM bloodline_profiles");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                UUID uniqueId = UUID.fromString(resultSet.getString("uuid"));
                loadProfile(uniqueId).ifPresent(profiles::add);
            }
        }
        return profiles;
    }

    @Override
    public void shutdown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    private String serializeMap(java.util.Map<?, ?> map) {
        StringBuilder builder = new StringBuilder();
        map.forEach((key, value) -> {
            if (!builder.isEmpty()) {
                builder.append(";");
            }
            builder.append(key).append("=").append(value);
        });
        return builder.toString();
    }

    private java.util.Map<String, String> deserializeMap(String raw) {
        java.util.Map<String, String> values = new java.util.HashMap<>();
        if (raw == null || raw.isBlank()) {
            return values;
        }
        for (String pair : raw.split(";")) {
            String[] split = pair.split("=", 2);
            if (split.length == 2) {
                values.put(split[0], split[1]);
            }
        }
        return values;
    }
}
