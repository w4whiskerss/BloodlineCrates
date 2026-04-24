package com.bloodline.crates.virtual;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VirtualInventoryStore implements Listener {
    private final BloodlineCrates plugin;
    private final Map<UUID, Map<String, Integer>> cache = new ConcurrentHashMap<>();

    public VirtualInventoryStore(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    public Map<String, Integer> get(UUID playerId) {
        return cache.computeIfAbsent(playerId, id -> new LinkedHashMap<>(plugin.getPlayerDataManager().getVirtualCrates(id)));
    }

    public void save(UUID playerId) {
        plugin.getPlayerDataManager().setVirtualCrates(playerId, get(playerId));
    }

    public void saveAndEvict(UUID playerId) {
        if (!cache.containsKey(playerId)) {
            return;
        }
        plugin.getPlayerDataManager().setVirtualCrates(playerId, cache.get(playerId));
        cache.remove(playerId);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        saveAndEvict(event.getPlayer().getUniqueId());
    }
}
