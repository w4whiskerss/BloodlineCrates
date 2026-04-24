package com.bloodline.crates.limit;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class LimitStore {
    private final File globalCountsFile;

    public LimitStore(BloodlineCrates plugin) {
        File folder = new File(plugin.getDataFolder(), "limits");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.globalCountsFile = new File(folder, "global_counts.yml");
    }

    public Map<String, Integer> loadGlobalCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (!globalCountsFile.exists()) {
            return counts;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(globalCountsFile);
        ConfigurationSection section = config.getConfigurationSection("counts");
        if (section == null) {
            return counts;
        }

        for (String crateId : section.getKeys(false)) {
            counts.put(crateId.toLowerCase(), Math.max(0, section.getInt(crateId, 0)));
        }
        return counts;
    }

    public void saveGlobalCounts(Map<String, Integer> counts) {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            config.set("counts." + entry.getKey(), Math.max(0, entry.getValue()));
        }
        try {
            config.save(globalCountsFile);
        } catch (Exception ignored) {
        }
    }
}
