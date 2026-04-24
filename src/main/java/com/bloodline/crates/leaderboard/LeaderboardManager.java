package com.bloodline.crates.leaderboard;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardManager {
    private final BloodlineCrates plugin;
    private final Map<LeaderboardPeriod, Map<UUID, LeaderboardEntry>> leaderboards = new EnumMap<>(LeaderboardPeriod.class);
    private final Map<LeaderboardPeriod, UUID> currentTopPlayers = new EnumMap<>(LeaderboardPeriod.class);
    private final Map<LeaderboardPeriod, Long> lastTopCheckTime = new EnumMap<>(LeaderboardPeriod.class);
    private BukkitTask saveTask;
    private BukkitTask resetTask;
    private boolean enabled;
    
    public LeaderboardManager(BloodlineCrates plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("leaderboards.enabled", true);
        
        if (!enabled) {
            plugin.getLogger().info("Leaderboards are disabled in config");
            return;
        }
        
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            leaderboards.put(period, new HashMap<>());
            lastTopCheckTime.put(period, System.currentTimeMillis());
        }
        
        loadAll();
        startTasks();
    }
    
    public void recordOpen(UUID playerUUID, String playerName) {
        if (!enabled) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            Map<UUID, LeaderboardEntry> periodLeaderboard = leaderboards.get(period);
            
            LeaderboardEntry entry = periodLeaderboard.get(playerUUID);
            if (entry == null) {
                entry = new LeaderboardEntry(playerUUID, playerName, 0, 0);
                periodLeaderboard.put(playerUUID, entry);
            }
            
            entry.setCrateOpens(entry.getCrateOpens() + 1);
            entry.setPlayerName(playerName);
            
            updateTimeAtTop(period, currentTime);
        }
    }
    
    private void updateTimeAtTop(LeaderboardPeriod period, long currentTime) {
        List<LeaderboardEntry> sorted = getLeaderboard(period, 1);
        if (sorted.isEmpty()) {
            return;
        }
        
        UUID currentTop = sorted.get(0).getPlayerUUID();
        UUID previousTop = currentTopPlayers.get(period);
        long lastCheckTime = lastTopCheckTime.get(period);
        
        if (previousTop != null && leaderboards.get(period).containsKey(previousTop)) {
            long elapsed = currentTime - lastCheckTime;
            LeaderboardEntry topEntry = leaderboards.get(period).get(previousTop);
            topEntry.setTimeAtTopMillis(topEntry.getTimeAtTopMillis() + elapsed);
        }
        
        currentTopPlayers.put(period, currentTop);
        lastTopCheckTime.put(period, currentTime);
    }
    
    public List<LeaderboardEntry> getLeaderboard(LeaderboardPeriod period, int topN) {
        if (!enabled) {
            return new ArrayList<>();
        }
        
        Map<UUID, LeaderboardEntry> periodLeaderboard = leaderboards.get(period);
        if (periodLeaderboard == null) {
            return new ArrayList<>();
        }
        
        return periodLeaderboard.values().stream()
            .sorted((a, b) -> Integer.compare(b.getCrateOpens(), a.getCrateOpens()))
            .limit(topN)
            .collect(Collectors.toList());
    }
    
    public int getPlayerOpens(UUID playerUUID, LeaderboardPeriod period) {
        if (!enabled) {
            return 0;
        }
        
        Map<UUID, LeaderboardEntry> periodLeaderboard = leaderboards.get(period);
        LeaderboardEntry entry = periodLeaderboard.get(playerUUID);
        return entry != null ? entry.getCrateOpens() : 0;
    }
    
    public int getPlayerRank(UUID playerUUID, LeaderboardPeriod period) {
        if (!enabled) {
            return 0;
        }
        
        List<LeaderboardEntry> sorted = getLeaderboard(period, Integer.MAX_VALUE);
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getPlayerUUID().equals(playerUUID)) {
                return i + 1;
            }
        }
        return 0;
    }
    
    public long getPlayerTimeAtTop(UUID playerUUID, LeaderboardPeriod period) {
        if (!enabled) {
            return 0;
        }
        
        Map<UUID, LeaderboardEntry> periodLeaderboard = leaderboards.get(period);
        LeaderboardEntry entry = periodLeaderboard.get(playerUUID);
        
        long totalTime = entry != null ? entry.getTimeAtTopMillis() : 0;
        
        UUID currentTop = currentTopPlayers.get(period);
        if (currentTop != null && currentTop.equals(playerUUID)) {
            long lastCheck = lastTopCheckTime.get(period);
            totalTime += (System.currentTimeMillis() - lastCheck);
        }
        
        return totalTime;
    }
    
    private void startTasks() {
        if (!enabled) {
            return;
        }
        
        int saveInterval = plugin.getConfig().getInt("leaderboards.save-interval-minutes", 5);
        long saveIntervalTicks = saveInterval * 60 * 20L;
        
        saveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::saveAll, saveIntervalTicks, saveIntervalTicks);
        
        resetTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkResets, 1200L, 1200L);
    }
    
    private void checkResets() {
        Calendar calendar = Calendar.getInstance();
        
        if (shouldResetDaily(calendar)) {
            resetPeriod(LeaderboardPeriod.DAILY);
        }
        
        if (shouldResetWeekly(calendar)) {
            resetPeriod(LeaderboardPeriod.WEEKLY);
        }
        
        if (shouldResetMonthly(calendar)) {
            resetPeriod(LeaderboardPeriod.MONTHLY);
        }
        
        if (shouldResetYearly(calendar)) {
            resetPeriod(LeaderboardPeriod.YEARLY);
        }
    }
    
    private boolean shouldResetDaily(Calendar calendar) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        return hour == 0 && minute == 0;
    }
    
    private boolean shouldResetWeekly(Calendar calendar) {
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        return dayOfWeek == Calendar.MONDAY && hour == 0 && minute == 0;
    }
    
    private boolean shouldResetMonthly(Calendar calendar) {
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        return dayOfMonth == 1 && hour == 0 && minute == 0;
    }
    
    private boolean shouldResetYearly(Calendar calendar) {
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        return dayOfYear == 1 && hour == 0 && minute == 0;
    }
    
    private void resetPeriod(LeaderboardPeriod period) {
        archivePeriod(period);
        leaderboards.get(period).clear();
        currentTopPlayers.remove(period);
        lastTopCheckTime.put(period, System.currentTimeMillis());
        plugin.getLogger().info("Reset " + period.name() + " leaderboard");
    }
    
    private void archivePeriod(LeaderboardPeriod period) {
        try {
            File archiveFolder = new File(plugin.getDataFolder(), "leaderboard/archive/" + period.name().toLowerCase());
            archiveFolder.mkdirs();
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            File archiveFile = new File(archiveFolder, timestamp + ".yml");
            
            YamlConfiguration config = new YamlConfiguration();
            savePeriodToConfig(config, period);
            config.save(archiveFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to archive " + period.name() + " leaderboard: " + e.getMessage());
        }
    }
    
    private void saveAll() {
        if (!enabled) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            updateTimeAtTop(period, currentTime);
        }
        
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            savePeriod(period);
        }
        if (plugin.getBroadcastManager() != null) {
            plugin.getBroadcastManager().saveStatsNow();
        }
    }
    
    private void savePeriod(LeaderboardPeriod period) {
        try {
            File leaderboardFolder = new File(plugin.getDataFolder(), "leaderboard");
            leaderboardFolder.mkdirs();
            
            File periodFile = new File(leaderboardFolder, period.name().toLowerCase() + ".yml");
            YamlConfiguration config = new YamlConfiguration();
            
            savePeriodToConfig(config, period);
            
            config.save(periodFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save " + period.name() + " leaderboard: " + e.getMessage());
        }
    }
    
    private void savePeriodToConfig(YamlConfiguration config, LeaderboardPeriod period) {
        Map<UUID, LeaderboardEntry> periodLeaderboard = leaderboards.get(period);
        
        int index = 0;
        for (LeaderboardEntry entry : periodLeaderboard.values()) {
            String path = "entries." + index;
            config.set(path + ".uuid", entry.getPlayerUUID().toString());
            config.set(path + ".name", entry.getPlayerName());
            config.set(path + ".opens", entry.getCrateOpens());
            config.set(path + ".time-at-top", entry.getTimeAtTopMillis());
            index++;
        }
    }
    
    private void loadAll() {
        if (!enabled) {
            return;
        }
        
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            loadPeriod(period);
        }
    }
    
    private void loadPeriod(LeaderboardPeriod period) {
        File leaderboardFolder = new File(plugin.getDataFolder(), "leaderboard");
        File periodFile = new File(leaderboardFolder, period.name().toLowerCase() + ".yml");
        
        if (!periodFile.exists()) {
            return;
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(periodFile);
        ConfigurationSection entriesSection = config.getConfigurationSection("entries");
        
        if (entriesSection == null) {
            return;
        }
        
        Map<UUID, LeaderboardEntry> periodLeaderboard = leaderboards.get(period);
        
        for (String key : entriesSection.getKeys(false)) {
            String uuidStr = config.getString("entries." + key + ".uuid");
            String name = config.getString("entries." + key + ".name");
            int opens = config.getInt("entries." + key + ".opens");
            long timeAtTop = config.getLong("entries." + key + ".time-at-top");
            
            try {
                UUID uuid = UUID.fromString(uuidStr);
                LeaderboardEntry entry = new LeaderboardEntry(uuid, name, opens, timeAtTop);
                periodLeaderboard.put(uuid, entry);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in leaderboard: " + uuidStr);
            }
        }
        
        plugin.getLogger().info("Loaded " + periodLeaderboard.size() + " entries for " + period.name() + " leaderboard");
    }
    
    public void shutdown() {
        if (!enabled) {
            return;
        }
        
        if (saveTask != null) {
            saveTask.cancel();
        }
        
        if (resetTask != null) {
            resetTask.cancel();
        }
        
        saveAll();
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
