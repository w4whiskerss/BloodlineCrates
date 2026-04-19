package com.bloodlinecrates.hook;

import com.bloodlinecrates.BloodlineCratesPlugin;
import com.bloodlinecrates.model.LeaderboardPeriod;
import com.bloodlinecrates.util.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public final class BloodlinePlaceholderExpansion extends PlaceholderExpansion {
    private final BloodlineCratesPlugin plugin;

    public BloodlinePlaceholderExpansion(BloodlineCratesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "bloodcrates";
    }

    @Override
    public String getAuthor() {
        return "OpenAI";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "";
        }
        if (params.equalsIgnoreCase("total_keys")) {
            return String.valueOf(plugin.keyManager().all().stream().mapToInt(key -> plugin.keyManager().getVirtual(player.getUniqueId(), key.id())).sum());
        }
        if (params.equalsIgnoreCase("crates_opened")) {
            return String.valueOf(plugin.playerDataManager().getOrLoad(player.getUniqueId()).totalCratesOpened());
        }
        if (params.equalsIgnoreCase("last_reward")) {
            return plugin.playerDataManager().getOrLoad(player.getUniqueId()).lastReward();
        }
        if (params.startsWith("keys_")) {
            return String.valueOf(plugin.keyManager().getVirtual(player.getUniqueId(), params.substring("keys_".length())));
        }
        if (params.startsWith("next_key_")) {
            return TimeUtil.formatMillis(plugin.keyManager().nextTimedKeyMillis(player.getUniqueId(), params.substring("next_key_".length())) - System.currentTimeMillis());
        }
        if (params.equalsIgnoreCase("time_until_next_key")) {
            long next = plugin.keyManager().all().stream()
                    .mapToLong(key -> plugin.keyManager().nextTimedKeyMillis(player.getUniqueId(), key.id()))
                    .filter(value -> value > 0L)
                    .min()
                    .orElse(0L);
            return TimeUtil.formatMillis(next - System.currentTimeMillis());
        }
        if (params.startsWith("lb_daily_")) {
            return plugin.analyticsManager().topPlayerName(LeaderboardPeriod.DAILY, Integer.parseInt(params.substring("lb_daily_".length())));
        }
        if (params.startsWith("lb_weekly_")) {
            return plugin.analyticsManager().topPlayerName(LeaderboardPeriod.WEEKLY, Integer.parseInt(params.substring("lb_weekly_".length())));
        }
        if (params.startsWith("lb_monthly_")) {
            return plugin.analyticsManager().topPlayerName(LeaderboardPeriod.MONTHLY, Integer.parseInt(params.substring("lb_monthly_".length())));
        }
        if (params.startsWith("lb_yearly_")) {
            return plugin.analyticsManager().topPlayerName(LeaderboardPeriod.YEARLY, Integer.parseInt(params.substring("lb_yearly_".length())));
        }
        return null;
    }
}
