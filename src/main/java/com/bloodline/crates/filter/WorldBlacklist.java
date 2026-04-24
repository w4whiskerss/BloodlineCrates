package com.bloodline.crates.filter;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class WorldBlacklist {
    private final BloodlineCrates plugin;
    private final Set<String> blacklistedWorlds = new HashSet<>();

    public WorldBlacklist(BloodlineCrates plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        blacklistedWorlds.clear();
        plugin.getConfig().getStringList("world-blacklist").stream()
            .map(world -> world.toLowerCase(Locale.ROOT))
            .forEach(blacklistedWorlds::add);
    }

    public boolean isBlacklisted(World world) {
        return world != null && blacklistedWorlds.contains(world.getName().toLowerCase(Locale.ROOT));
    }

    public void notifyBlocked(Player player) {
        player.sendMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMessage("world-blacklisted"));
    }
}
