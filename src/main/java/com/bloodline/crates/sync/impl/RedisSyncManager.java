package com.bloodline.crates.sync.impl;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.debug.DebugEventType;
import com.bloodline.crates.sync.SyncManager;
import com.bloodline.crates.sync.SyncPayload;
import org.bukkit.Bukkit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class RedisSyncManager implements SyncManager {
    private static final String CHANNEL = "bloodcrates:events";

    private final BloodlineCrates plugin;
    private Consumer<SyncPayload> handler = payload -> {};
    private Thread subscriberThread;
    private JedisPubSub pubSub;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public RedisSyncManager(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public void publishKeyGrant(UUID playerUUID, String crateId, int amount) {
        plugin.getDebugManager().emit(DebugEventType.SYNC_EVENT, playerUUID,
            () -> "publish KEY_GIVEN crate=" + crateId + ", amount=" + amount);
        publish(new SyncPayload(serverId(), "KEY_GIVEN", playerUUID + "|" + crateId + "|" + amount, System.currentTimeMillis()));
    }

    @Override
    public void publishRewardGrant(UUID playerUUID, String crateId, String rewardSummary) {
        plugin.getDebugManager().emit(DebugEventType.SYNC_EVENT, playerUUID,
            () -> "publish REWARD_GRANTED crate=" + crateId + ", reward=" + rewardSummary);
        publish(new SyncPayload(serverId(), "REWARD_GRANTED", playerUUID + "|" + crateId + "|" + rewardSummary, System.currentTimeMillis()));
    }

    @Override
    public void subscribeToUpdates(Consumer<SyncPayload> handler) {
        this.handler = handler;
        if (subscriberThread != null) {
            return;
        }

        subscriberThread = new Thread(() -> {
            try (Jedis jedis = createJedis()) {
                pubSub = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        SyncPayload payload = SyncPayload.fromJson(message);
                        Bukkit.getScheduler().runTask(plugin, () -> RedisSyncManager.this.handler.accept(payload));
                    }
                };
                jedis.subscribe(pubSub, CHANNEL);
            } catch (Exception exception) {
                if (running.get()) {
                    plugin.getLogger().warning("Redis sync subscriber stopped: " + exception.getMessage());
                }
            }
        }, "BloodlineCrates-RedisSync");
        subscriberThread.setDaemon(true);
        subscriberThread.start();
    }

    @Override
    public void shutdown() {
        running.set(false);
        if (pubSub != null) {
            try {
                pubSub.unsubscribe();
            } catch (Exception ignored) {
            }
        }
    }

    private void publish(SyncPayload payload) {
        try (Jedis jedis = createJedis()) {
            jedis.publish(CHANNEL, payload.toJson());
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to publish Redis sync event: " + exception.getMessage());
        }
    }

    private Jedis createJedis() {
        String host = plugin.getConfig().getString("sync.redis.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("sync.redis.port", 6379);
        String password = plugin.getConfig().getString("sync.redis.password", "");
        Jedis jedis = new Jedis(host, port);
        if (password != null && !password.isBlank()) {
            jedis.auth(password);
        }
        return jedis;
    }

    private String serverId() {
        return plugin.getConfig().getString("sync.server-id", "local");
    }
}
