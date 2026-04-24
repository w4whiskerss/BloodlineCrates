package com.bloodline.crates.limit;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.debug.DebugEventType;
import com.bloodline.crates.model.Crate;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class LimitManager {
    private final BloodlineCrates plugin;
    private final LimitStore store;
    private final Map<String, Integer> globalCounts = new LinkedHashMap<>();
    private BukkitTask saveTask;
    private BukkitTask resetTask;

    public LimitManager(BloodlineCrates plugin, LimitStore store) {
        this.plugin = plugin;
        this.store = store;
        this.globalCounts.putAll(store.loadGlobalCounts());
        startTasks();
    }

    public LimitCheckResult canOpen(Player player, Crate crate) {
        if (crate.isGlobalLimitEnabled()
            && crate.getGlobalLimitMax() > 0
            && getGlobalOpens(crate.getId()) >= crate.getGlobalLimitMax()
            && isDisableOnReached(crate)) {
            LimitCheckResult result = new LimitCheckResult(false, LimitType.GLOBAL, crate.getGlobalLimitMessage());
            plugin.getDebugManager().emit(DebugEventType.LIMIT_CHECK, player.getUniqueId(),
                () -> "crate=" + crate.getId() + ", type=GLOBAL, count=" + getGlobalOpens(crate.getId())
                    + "/" + crate.getGlobalLimitMax() + ", allowed=false");
            return result;
        }

        if (crate.isPerPlayerLimitEnabled()
            && crate.getPerPlayerLimitMax() > 0
            && getPlayerOpens(player.getUniqueId(), crate.getId()) >= crate.getPerPlayerLimitMax()) {
            LimitCheckResult result = new LimitCheckResult(false, LimitType.PER_PLAYER, crate.getPerPlayerLimitMessage());
            plugin.getDebugManager().emit(DebugEventType.LIMIT_CHECK, player.getUniqueId(),
                () -> "crate=" + crate.getId() + ", type=PER_PLAYER, count=" + getPlayerOpens(player.getUniqueId(), crate.getId())
                    + "/" + crate.getPerPlayerLimitMax() + ", allowed=false");
            return result;
        }

        plugin.getDebugManager().emit(DebugEventType.LIMIT_CHECK, player.getUniqueId(),
            () -> "crate=" + crate.getId() + ", allowed=true");
        return new LimitCheckResult(true, null, null);
    }

    public void recordOpen(Player player, Crate crate) {
        String crateId = normalize(crate.getId());
        boolean wasSoldOut = isSoldOut(crate);
        globalCounts.put(crateId, getGlobalOpens(crateId) + 1);
        store.saveGlobalCounts(globalCounts);
        plugin.getPlayerDataManager().incrementCrateOpenCount(player.getUniqueId(), crateId);

        if (isSoldOut(crate)) {
            if (!wasSoldOut && plugin.getDiscordBotManager() != null) {
                plugin.getDiscordBotManager().getAlertManager().alertGlobalLimitReached(crate);
            }
            plugin.getPlacementManager().refreshPlacements();
        }
    }

    public void resetLimit(String crateId, LimitType type) {
        if (type == LimitType.GLOBAL) {
            globalCounts.remove(normalize(crateId));
            store.saveGlobalCounts(globalCounts);
            plugin.getPlacementManager().refreshPlacements();
        }
    }

    public void resetPlayerLimit(String crateId, UUID playerUUID) {
        plugin.getPlayerDataManager().setCrateOpenCount(playerUUID, normalize(crateId), 0);
    }

    public void resetAllPlayerLimits(String crateId) {
        String normalized = normalize(crateId);
        File folder = new File(plugin.getDataFolder(), "playerdata");
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        archiveCrateCounts(normalized, files);
        for (File file : files) {
            try {
                UUID playerId = UUID.fromString(file.getName().replace(".yml", ""));
                plugin.getPlayerDataManager().setCrateOpenCount(playerId, normalized, 0);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public int getGlobalOpens(String crateId) {
        return globalCounts.getOrDefault(normalize(crateId), 0);
    }

    public int getPlayerOpens(UUID playerUUID, String crateId) {
        return plugin.getPlayerDataManager().getCrateOpenCount(playerUUID, normalize(crateId));
    }

    public boolean isSoldOut(Crate crate) {
        return crate.isGlobalLimitEnabled()
            && crate.getGlobalLimitMax() > 0
            && getGlobalOpens(crate.getId()) >= crate.getGlobalLimitMax()
            && isDisableOnReached(crate);
    }

    public void shutdown() {
        if (saveTask != null) {
            saveTask.cancel();
        }
        if (resetTask != null) {
            resetTask.cancel();
        }
        store.saveGlobalCounts(globalCounts);
    }

    private void startTasks() {
        saveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> store.saveGlobalCounts(globalCounts), 6000L, 6000L);
        resetTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkResets, 1200L, 1200L);
    }

    private void checkResets() {
        Calendar calendar = Calendar.getInstance();
        if (calendar.get(Calendar.MINUTE) != 0) {
            return;
        }

        boolean daily = calendar.get(Calendar.HOUR_OF_DAY) == 0;
        boolean weekly = daily && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY;
        boolean monthly = daily && calendar.get(Calendar.DAY_OF_MONTH) == 1;

        for (Crate crate : plugin.getConfigManager().getAllCrates()) {
            String interval = crate.getPerPlayerResetInterval();
            if ("DAILY".equalsIgnoreCase(interval) && daily) {
                resetAllPlayerLimits(crate.getId());
            } else if ("WEEKLY".equalsIgnoreCase(interval) && weekly) {
                resetAllPlayerLimits(crate.getId());
            } else if ("MONTHLY".equalsIgnoreCase(interval) && monthly) {
                resetAllPlayerLimits(crate.getId());
            }
        }
    }

    private void archiveCrateCounts(String crateId, File[] playerFiles) {
        File archiveFolder = new File(plugin.getDataFolder(), "limits/archive");
        if (!archiveFolder.exists()) {
            archiveFolder.mkdirs();
        }

        YamlConfiguration archive = new YamlConfiguration();
        for (File file : playerFiles) {
            try {
                UUID playerId = UUID.fromString(file.getName().replace(".yml", ""));
                int count = plugin.getPlayerDataManager().getCrateOpenCount(playerId, crateId);
                if (count > 0) {
                    archive.set("players." + playerId, count);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (archive.getKeys(true).isEmpty()) {
            return;
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        try {
            archive.save(new File(archiveFolder, crateId + "-" + timestamp + ".yml"));
        } catch (Exception ignored) {
        }
    }

    private boolean isDisableOnReached(Crate crate) {
        return "DISABLE".equalsIgnoreCase(crate.getGlobalLimitOnReached());
    }

    private String normalize(String crateId) {
        return crateId == null ? "" : crateId.toLowerCase(Locale.ROOT);
    }

    public record LimitCheckResult(boolean allowed, LimitType reason, String message) {
    }
}
