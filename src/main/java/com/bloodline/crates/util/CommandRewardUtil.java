package com.bloodline.crates.util;

import com.bloodline.crates.BloodlineCrates;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CommandRewardUtil {
    private CommandRewardUtil() {
    }

    public static String resolveCommand(BloodlineCrates plugin, Player player, String command) {
        if (command == null) {
            return "";
        }

        String resolved = command.trim().replaceFirst("^/+", "");
        resolved = resolved
            .replace("%player%", player.getName())
            .replace("{player}", player.getName())
            .replace("%player_name%", player.getName())
            .replace("{player_name}", player.getName())
            .replace("%player_uuid%", player.getUniqueId().toString())
            .replace("{player_uuid}", player.getUniqueId().toString())
            .replace("%uuid%", player.getUniqueId().toString())
            .replace("{uuid}", player.getUniqueId().toString());

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            resolved = PlaceholderAPI.setPlaceholders(player, resolved);
        }

        return resolved;
    }
}
