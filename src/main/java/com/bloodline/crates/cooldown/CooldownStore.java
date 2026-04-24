package com.bloodline.crates.cooldown;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class CooldownStore {
    private final BloodlineCrates plugin;
    private final File file;

    public CooldownStore(BloodlineCrates plugin) {
        this.plugin = plugin;
        File folder = new File(plugin.getDataFolder(), "cooldowns");
        folder.mkdirs();
        this.file = new File(folder, "cooldowns.yml");
    }

    public List<CooldownEntry> load() {
        List<CooldownEntry> entries = new ArrayList<>();
        if (!file.exists()) {
            return entries;
        }

        long now = System.currentTimeMillis();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<?> raw = config.getList("cooldowns");
        if (raw == null) {
            return entries;
        }

        for (Object entry : raw) {
            if (!(entry instanceof java.util.Map<?, ?> map)) {
                continue;
            }

            try {
                UUID playerUUID = UUID.fromString(String.valueOf(map.get("player")));
                String crateId = String.valueOf(map.get("crate-id"));
                long lastOpenedAt = longValue(map.get("last-opened-at"));
                long cooldownMillis = longValue(map.get("cooldown-millis"));
                if (lastOpenedAt + cooldownMillis < now) {
                    continue;
                }
                entries.add(new CooldownEntry(playerUUID, crateId, lastOpenedAt, cooldownMillis));
            } catch (Exception exception) {
                plugin.getLogger().warning("Failed to load cooldown entry: " + exception.getMessage());
            }
        }

        return entries;
    }

    public void save(Collection<CooldownEntry> entries) {
        long now = System.currentTimeMillis();
        List<java.util.Map<String, Object>> serialized = new ArrayList<>();
        for (CooldownEntry entry : entries) {
            if (entry.getLastOpenedAt() + entry.getCooldownMillis() < now) {
                continue;
            }
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("player", entry.getPlayerUUID().toString());
            map.put("crate-id", entry.getCrateId());
            map.put("last-opened-at", entry.getLastOpenedAt());
            map.put("cooldown-millis", entry.getCooldownMillis());
            serialized.add(map);
        }

        YamlConfiguration config = new YamlConfiguration();
        config.set("cooldowns", serialized);
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save cooldowns: " + exception.getMessage());
        }
    }

    private long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }
}
