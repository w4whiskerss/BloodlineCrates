package com.bloodline.crates.placement;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class HologramHandler {
    private final BloodlineCrates plugin;
    private final boolean decentHologramsAvailable;
    private boolean loggedMissing;

    public HologramHandler(BloodlineCrates plugin) {
        this.plugin = plugin;
        this.decentHologramsAvailable = Bukkit.getPluginManager().getPlugin("DecentHolograms") != null;
        if (!decentHologramsAvailable) {
            plugin.getLogger().info("DecentHolograms not found. Hologram support is disabled.");
            loggedMissing = true;
        }
    }

    public String createHologram(String name, Location location, List<String> lines) {
        if (!decentHologramsAvailable) {
            return null;
        }

        try {
            Class<?> dhapiClass = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
            Method createMethod = dhapiClass.getMethod("createHologram", String.class, Location.class, List.class);
            createMethod.invoke(null, name, location, lines);
            return name;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to create DecentHolograms hologram. Falling back to no-op.", exception);
            return null;
        }
    }

    public void updateHologram(String hologramId, List<String> lines) {
        if (!decentHologramsAvailable || hologramId == null) {
            return;
        }

        try {
            Class<?> dhapiClass = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
            Method getMethod = dhapiClass.getMethod("getHologram", String.class);
            Object hologram = getMethod.invoke(null, hologramId);
            if (hologram == null) {
                return;
            }

            Method setLines = dhapiClass.getMethod("setHologramLines", Class.forName("eu.decentsoftware.holograms.api.holograms.Hologram"), List.class);
            setLines.invoke(null, hologram, lines);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to update DecentHolograms hologram.", exception);
        }
    }

    public void deleteHologram(String hologramId) {
        if (!decentHologramsAvailable || hologramId == null) {
            return;
        }

        try {
            Class<?> dhapiClass = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
            Method getMethod = dhapiClass.getMethod("getHologram", String.class);
            Object hologram = getMethod.invoke(null, hologramId);
            if (hologram == null) {
                return;
            }

            Method removeMethod = dhapiClass.getMethod("removeHologram", Class.forName("eu.decentsoftware.holograms.api.holograms.Hologram"));
            removeMethod.invoke(null, hologram);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete DecentHolograms hologram.", exception);
        }
    }

    public void deleteAll() {
        if (!decentHologramsAvailable) {
            if (!loggedMissing) {
                plugin.getLogger().info("DecentHolograms not found. Hologram support is disabled.");
                loggedMissing = true;
            }
            return;
        }
        // Holograms are deleted by tracked placement IDs elsewhere.
    }

    public String generateName() {
        return "bloodlinecrate_" + UUID.randomUUID();
    }
}
