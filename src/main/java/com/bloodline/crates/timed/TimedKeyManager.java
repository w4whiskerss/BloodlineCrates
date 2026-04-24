package com.bloodline.crates.timed;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimedKeyManager {
    private final BloodlineCrates plugin;
    private final List<TimedKeyConfig> configs = new ArrayList<>();
    private final Map<TimedKeyConfig, BukkitTask> tasks = new HashMap<>();
    
    public TimedKeyManager(BloodlineCrates plugin) {
        this.plugin = plugin;
        loadConfigs();
        startTasks();
    }
    
    private void loadConfigs() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("timed-keys");
        if (section == null) {
            return;
        }
        
        for (String key : section.getKeys(false)) {
            ConfigurationSection configSection = section.getConfigurationSection(key);
            if (configSection == null) {
                continue;
            }
            
            String crateId = configSection.getString("crate-id");
            int amount = configSection.getInt("amount", 1);
            String intervalStr = configSection.getString("interval", "DAILY");
            long intervalTicks = configSection.getLong("interval-ticks", 72000);
            
            TimedKeyInterval interval;
            try {
                interval = TimedKeyInterval.valueOf(intervalStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid timed key interval: " + intervalStr + ", using DAILY");
                interval = TimedKeyInterval.DAILY;
            }
            
            TimedKeyConfig config = new TimedKeyConfig(crateId, amount, interval, intervalTicks);
            configs.add(config);
        }
        
        plugin.getLogger().info("Loaded " + configs.size() + " timed key configuration(s)");
    }
    
    private void startTasks() {
        for (TimedKeyConfig config : configs) {
            long period = getTaskPeriod(config);
            TimedKeyTask task = new TimedKeyTask(plugin, config);
            BukkitTask bukkitTask = task.runTaskTimer(plugin, period, period);
            tasks.put(config, bukkitTask);
        }
    }
    
    private long getTaskPeriod(TimedKeyConfig config) {
        switch (config.getInterval()) {
            case HOURLY:
                return 72000L;
            case DAILY:
                return 1728000L;
            case INTERVAL:
                return config.getIntervalTicks();
            default:
                return 72000L;
        }
    }
    
    public void shutdown() {
        for (BukkitTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
    }

    public void reload() {
        shutdown();
        configs.clear();
        loadConfigs();
        startTasks();
    }
    
    public long getTimeUntilNext(Player player, String crateId) {
        for (TimedKeyConfig config : configs) {
            if (config.getCrateId().equals(crateId)) {
                long lastGrant = plugin.getPlayerDataManager().getTimedKeyTimestamp(
                    player.getUniqueId(), 
                    crateId, 
                    config.getInterval()
                );
                
                if (lastGrant == 0) {
                    return 0;
                }
                
                long currentTime = System.currentTimeMillis();
                long elapsedTicks = (currentTime - lastGrant) / 50;
                long requiredTicks = getTaskPeriod(config);
                long remaining = requiredTicks - elapsedTicks;
                
                return Math.max(0, remaining * 50);
            }
        }
        
        return 0;
    }
}
