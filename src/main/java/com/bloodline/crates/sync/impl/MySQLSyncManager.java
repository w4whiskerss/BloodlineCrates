package com.bloodline.crates.sync.impl;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.debug.DebugEventType;
import com.bloodline.crates.sync.SyncManager;
import com.bloodline.crates.sync.SyncPayload;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import java.util.function.Consumer;

public class MySQLSyncManager implements SyncManager {
    private final BloodlineCrates plugin;
    private final HikariDataSource dataSource;
    private Consumer<SyncPayload> handler = payload -> {};
    private long lastSeenId = 0L;
    private int taskId = -1;

    public MySQLSyncManager(BloodlineCrates plugin) {
        this.plugin = plugin;
        this.dataSource = createDataSource();
        createTableIfNeeded();
    }

    @Override
    public void publishKeyGrant(UUID playerUUID, String crateId, int amount) {
        plugin.getDebugManager().emit(DebugEventType.SYNC_EVENT, playerUUID,
            () -> "publish KEY_GIVEN crate=" + crateId + ", amount=" + amount);
        publish("KEY_GIVEN", playerUUID + "|" + crateId + "|" + amount);
    }

    @Override
    public void publishRewardGrant(UUID playerUUID, String crateId, String rewardSummary) {
        plugin.getDebugManager().emit(DebugEventType.SYNC_EVENT, playerUUID,
            () -> "publish REWARD_GRANTED crate=" + crateId + ", reward=" + rewardSummary);
        publish("REWARD_GRANTED", playerUUID + "|" + crateId + "|" + rewardSummary);
    }

    @Override
    public void subscribeToUpdates(Consumer<SyncPayload> handler) {
        this.handler = handler;
        if (taskId != -1) {
            return;
        }

        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::pollUpdates, 100L, 100L).getTaskId();
    }

    @Override
    public void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        dataSource.close();
    }

    private void publish(String eventType, String payloadJson) {
        String sql = "INSERT INTO bc_sync_events (server_id, event_type, payload) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serverId());
            statement.setString(2, eventType);
            statement.setString(3, payloadJson);
            statement.executeUpdate();
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to publish MySQL sync event: " + exception.getMessage());
        }
    }

    private void pollUpdates() {
        String sql = "SELECT id, server_id, event_type, payload, UNIX_TIMESTAMP(created_at) * 1000 AS created_ms FROM bc_sync_events WHERE id > ? AND server_id <> ? ORDER BY id ASC";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, lastSeenId);
            statement.setString(2, serverId());
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                lastSeenId = resultSet.getLong("id");
                SyncPayload payload = new SyncPayload(
                    resultSet.getString("server_id"),
                    resultSet.getString("event_type"),
                    resultSet.getString("payload"),
                    resultSet.getLong("created_ms")
                );
                Bukkit.getScheduler().runTask(plugin, () -> handler.accept(payload));
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to poll MySQL sync events: " + exception.getMessage());
        }
    }

    private HikariDataSource createDataSource() {
        String host = plugin.getConfig().getString("sync.mysql.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("sync.mysql.port", 3306);
        String database = plugin.getConfig().getString("sync.mysql.database", "bloodlinecrates");
        String user = plugin.getConfig().getString("sync.mysql.user", "root");
        String password = plugin.getConfig().getString("sync.mysql.password", "");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true");
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(4);
        return new HikariDataSource(config);
    }

    private void createTableIfNeeded() {
        String sql = "CREATE TABLE IF NOT EXISTS bc_sync_events ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
            + "server_id VARCHAR(64) NOT NULL,"
            + "event_type VARCHAR(32) NOT NULL,"
            + "payload JSON NOT NULL,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
            + ")";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to ensure MySQL sync schema exists: " + exception.getMessage());
        }
    }

    private String serverId() {
        return plugin.getConfig().getString("sync.server-id", "local");
    }
}
