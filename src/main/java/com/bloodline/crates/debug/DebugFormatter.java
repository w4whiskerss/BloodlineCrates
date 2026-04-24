package com.bloodline.crates.debug;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.ChatColor;

public class DebugFormatter {
    private final BloodlineCrates plugin;

    public DebugFormatter(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    public String format(DebugEvent event) {
        String colour = ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("debug.colour", "&6"));
        return colour + "[BloodCrates Debug] "
            + ChatColor.YELLOW + event.type().name()
            + ChatColor.GRAY + " | "
            + ChatColor.WHITE + event.detail();
    }
}
