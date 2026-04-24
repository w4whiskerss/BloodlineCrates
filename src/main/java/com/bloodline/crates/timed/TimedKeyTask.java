package com.bloodline.crates.timed;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.debug.DebugEventType;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.CrateKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Optional;

public class TimedKeyTask extends BukkitRunnable {
    private final BloodlineCrates plugin;
    private final TimedKeyConfig config;

    public TimedKeyTask(BloodlineCrates plugin, TimedKeyConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public void run() {
        long currentTime = System.currentTimeMillis();

        Optional<Crate> crateOpt = plugin.getConfigManager().getCrate(config.getCrateId());
        if (crateOpt.isEmpty()) {
            plugin.getLogger().warning("Timed key config references unknown crate: " + config.getCrateId());
            return;
        }

        Crate crate = crateOpt.get();
        ItemStack keyItem = crate.getKeyDisplayItem().clone();
        CrateKey crateKey = new CrateKey(crate.getId(), keyItem);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isEligible(player, currentTime)) {
                if (plugin.getWorldBlacklist().isBlacklisted(player.getWorld())) {
                    plugin.getWorldBlacklist().notifyBlocked(player);
                    continue;
                }

                plugin.getKeyManager().giveKey(player, crateKey, config.getAmount());
                plugin.getPlayerDataManager().setTimedKeyTimestamp(
                    player.getUniqueId(),
                    config.getCrateId(),
                    config.getInterval(),
                    currentTime
                );
                plugin.getDebugManager().emit(DebugEventType.TIMED_KEY_GRANT, player.getUniqueId(),
                    () -> "crate=" + crate.getId() + ", amount=" + config.getAmount() + ", interval=" + config.getInterval());
            }
        }
    }

    private boolean isEligible(Player player, long currentTime) {
        long lastGrant = plugin.getPlayerDataManager().getTimedKeyTimestamp(
            player.getUniqueId(),
            config.getCrateId(),
            config.getInterval()
        );

        if (lastGrant == 0) {
            return true;
        }

        long elapsedTicks = (currentTime - lastGrant) / 50;
        long requiredTicks = getRequiredTicks();

        return elapsedTicks >= requiredTicks;
    }

    private long getRequiredTicks() {
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
}
