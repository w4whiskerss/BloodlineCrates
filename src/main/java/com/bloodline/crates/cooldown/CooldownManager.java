package com.bloodline.crates.cooldown;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.debug.DebugEventType;
import com.bloodline.crates.model.Crate;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class CooldownManager {
    private final BloodlineCrates plugin;
    private final CooldownStore store;
    private final Map<String, CooldownEntry> entries = new HashMap<>();

    public CooldownManager(BloodlineCrates plugin) {
        this.plugin = plugin;
        this.store = new CooldownStore(plugin);
        for (CooldownEntry entry : store.load()) {
            entries.put(key(entry.getPlayerUUID(), entry.getCrateId()), entry);
        }
    }

    public boolean isOnCooldown(UUID playerUUID, Crate crate) {
        long remainingMillis = getRemainingMillis(playerUUID, crate);
        boolean onCooldown = remainingMillis > 0L;
        plugin.getDebugManager().emit(DebugEventType.COOLDOWN_CHECK, playerUUID,
            () -> "crate=" + (crate == null ? "unknown" : crate.getId()) + ", remaining=" + remainingMillis + "ms, blocked=" + onCooldown);
        return onCooldown;
    }

    public long getRemainingMillis(UUID playerUUID, Crate crate) {
        if (crate == null || !crate.isCooldownEnabled() || crate.getCooldownDurationSeconds() <= 0L) {
            return 0L;
        }
        CooldownEntry entry = entries.get(key(playerUUID, crate.getId()));
        if (entry == null) {
            return 0L;
        }
        long remaining = (entry.getLastOpenedAt() + entry.getCooldownMillis()) - System.currentTimeMillis();
        if (remaining <= 0L) {
            entries.remove(key(playerUUID, crate.getId()));
            save();
            return 0L;
        }
        return remaining;
    }

    public void applyCooldown(UUID playerUUID, Crate crate) {
        if (crate == null || !crate.isCooldownEnabled() || crate.getCooldownDurationSeconds() <= 0L) {
            return;
        }
        long cooldownMillis = TimeUnit.SECONDS.toMillis(crate.getCooldownDurationSeconds());
        entries.put(key(playerUUID, crate.getId()), new CooldownEntry(playerUUID, crate.getId(), System.currentTimeMillis(), cooldownMillis));
        save();
    }

    public void clearCooldown(UUID playerUUID, String crateId) {
        entries.remove(key(playerUUID, crateId));
        save();
    }

    public void clearAllCooldowns(String crateId) {
        entries.entrySet().removeIf(entry -> entry.getValue().getCrateId().equalsIgnoreCase(crateId));
        save();
    }

    public void resetAllCooldowns() {
        entries.clear();
        save();
    }

    public String formatRemaining(long millis) {
        if (millis <= 0L) {
            return "Ready";
        }
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void save() {
        store.save(entries.values());
    }

    private String key(UUID playerUUID, String crateId) {
        return playerUUID + ":" + crateId.toLowerCase(Locale.ROOT);
    }
}
