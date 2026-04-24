package com.bloodline.crates.debug;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class DebugManager implements Listener {
    private final BloodlineCrates plugin;
    private final DebugFormatter formatter;
    private final Set<UUID> activeDebugAdmins = new HashSet<>();
    private final Set<UUID> allPlayerDebugAdmins = new HashSet<>();
    private final Map<UUID, UUID> targetPlayers = new HashMap<>();

    public DebugManager(BloodlineCrates plugin) {
        this.plugin = plugin;
        this.formatter = new DebugFormatter(plugin);
    }

    public void enableDebug(UUID adminUUID, UUID targetPlayerUUID) {
        activeDebugAdmins.add(adminUUID);
        if (targetPlayerUUID == null) {
            allPlayerDebugAdmins.add(adminUUID);
            targetPlayers.remove(adminUUID);
            return;
        }

        allPlayerDebugAdmins.remove(adminUUID);
        targetPlayers.put(adminUUID, targetPlayerUUID);
    }

    public void disableDebug(UUID adminUUID) {
        activeDebugAdmins.remove(adminUUID);
        allPlayerDebugAdmins.remove(adminUUID);
        targetPlayers.remove(adminUUID);
    }

    public boolean isDebugging(UUID adminUUID) {
        return activeDebugAdmins.contains(adminUUID);
    }

    public UUID getTargetPlayer(UUID adminUUID) {
        return targetPlayers.get(adminUUID);
    }

    public boolean isDebuggingAllPlayers(UUID adminUUID) {
        return allPlayerDebugAdmins.contains(adminUUID);
    }

    public void emit(DebugEventType type, UUID subjectPlayerUUID, String detail) {
        emit(type, subjectPlayerUUID, () -> detail);
    }

    public void emit(DebugEventType type, UUID subjectPlayerUUID, Supplier<String> detailSupplier) {
        if (activeDebugAdmins.isEmpty()) {
            return;
        }

        DebugEvent event = null;
        for (UUID adminUUID : Set.copyOf(activeDebugAdmins)) {
            if (!matches(adminUUID, subjectPlayerUUID)) {
                continue;
            }

            Player admin = Bukkit.getPlayer(adminUUID);
            if (admin == null || !admin.isOnline()) {
                disableDebug(adminUUID);
                continue;
            }

            if (event == null) {
                event = new DebugEvent(type, subjectPlayerUUID, detailSupplier.get(), System.currentTimeMillis());
            }
            admin.sendMessage(formatter.format(event));
        }
    }

    private boolean matches(UUID adminUUID, UUID subjectPlayerUUID) {
        if (allPlayerDebugAdmins.contains(adminUUID)) {
            return true;
        }
        UUID target = targetPlayers.get(adminUUID);
        return target != null && target.equals(subjectPlayerUUID);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        disableDebug(event.getPlayer().getUniqueId());
    }
}
