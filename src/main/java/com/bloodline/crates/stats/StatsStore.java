package com.bloodline.crates.stats;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class StatsStore {
    private final BloodlineCrates plugin;
    private final File statsFile;

    public StatsStore(BloodlineCrates plugin) {
        this.plugin = plugin;
        File statsFolder = new File(plugin.getDataFolder(), "stats");
        if (!statsFolder.exists()) {
            statsFolder.mkdirs();
        }
        this.statsFile = new File(statsFolder, "server_stats.yml");
    }

    public Map<String, Integer> loadServerOpens() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (!statsFile.exists()) {
            return counts;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(statsFile);
        ConfigurationSection section = config.getConfigurationSection("server-opens");
        if (section == null) {
            return counts;
        }

        for (String crateId : section.getKeys(false)) {
            counts.put(normalize(crateId), Math.max(0, section.getInt(crateId, 0)));
        }
        return counts;
    }

    public void saveServerOpens(Map<String, Integer> counts) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(statsFile);
        config.set("server-opens", null);
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            config.set("server-opens." + normalize(entry.getKey()), Math.max(0, entry.getValue()));
        }

        try {
            config.save(statsFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save server stats: " + exception.getMessage());
        }
    }

    private String normalize(String crateId) {
        return crateId == null ? "" : crateId.toLowerCase(Locale.ROOT);
    }
}
