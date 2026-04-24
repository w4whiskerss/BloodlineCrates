package com.bloodline.crates.virtual;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class VirtualInventoryManager {
    private final BloodlineCrates plugin;
    private final VirtualInventoryStore store;

    public VirtualInventoryManager(BloodlineCrates plugin, VirtualInventoryStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public void addVirtualCrate(UUID playerUUID, String crateId, int amount) {
        if (playerUUID == null || crateId == null || crateId.isBlank() || amount <= 0) {
            return;
        }
        Map<String, Integer> crates = new LinkedHashMap<>(store.get(playerUUID));
        String normalizedId = crateId.toLowerCase(Locale.ROOT);
        crates.put(normalizedId, crates.getOrDefault(normalizedId, 0) + amount);
        store.get(playerUUID).clear();
        store.get(playerUUID).putAll(crates);
        store.save(playerUUID);
    }

    public boolean removeVirtualCrate(UUID playerUUID, String crateId, int amount) {
        if (playerUUID == null || crateId == null || crateId.isBlank() || amount <= 0) {
            return false;
        }

        Map<String, Integer> crates = new LinkedHashMap<>(store.get(playerUUID));
        String normalizedId = crateId.toLowerCase(Locale.ROOT);
        int current = crates.getOrDefault(normalizedId, 0);
        if (current < amount) {
            return false;
        }

        if (current == amount) {
            crates.remove(normalizedId);
        } else {
            crates.put(normalizedId, current - amount);
        }

        store.get(playerUUID).clear();
        store.get(playerUUID).putAll(crates);
        store.save(playerUUID);
        return true;
    }

    public int getVirtualCrateCount(UUID playerUUID, String crateId) {
        if (playerUUID == null || crateId == null || crateId.isBlank()) {
            return 0;
        }
        return store.get(playerUUID).getOrDefault(crateId.toLowerCase(Locale.ROOT), 0);
    }

    public Map<String, Integer> getAllVirtualCrates(UUID playerUUID) {
        return new LinkedHashMap<>(store.get(playerUUID));
    }

    public void openGUI(Player player) {
        openGUI(player, player, false);
    }

    public void openGUI(Player viewer, Player owner, boolean readOnly) {
        plugin.getGuiManager().openVirtualInventory(viewer, owner, readOnly);
    }
}
