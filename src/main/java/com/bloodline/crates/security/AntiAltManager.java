package com.bloodline.crates.security;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

public class AntiAltManager implements Listener {
    private final BloodlineCrates plugin;
    private final IPTracker ipTracker;

    public AntiAltManager(BloodlineCrates plugin) {
        this.plugin = plugin;
        this.ipTracker = new IPTracker(plugin);
    }

    public boolean isAltAccount(UUID playerId) {
        List<String> whitelist = plugin.getConfig().getStringList("security.anti-alt.whitelist");
        if (whitelist.contains(playerId.toString())) {
            return false;
        }

        int maxAccounts = Math.max(1, plugin.getConfig().getInt("security.anti-alt.max-accounts-per-ip", 3));
        return ipTracker.getAccountCount(playerId) > maxAccounts;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        InetSocketAddress address = player.getAddress();
        if (address == null || address.getAddress() == null) {
            return;
        }

        ipTracker.recordPlayer(player.getUniqueId(), address.getAddress().getHostAddress());
        if (!isAltAccount(player.getUniqueId())) {
            return;
        }

        handleDetection(player);
    }

    private void handleDetection(Player player) {
        List<String> actions = plugin.getConfig().getStringList("security.anti-alt.actions");
        if (actions.isEmpty()) {
            actions = List.of("FLAG_ONLY");
        }

        String adminMessage = plugin.getConfigManager().getPrefix() +
            plugin.getConfigManager().getMessage("alt-detected")
                .replace("{player}", player.getName())
                .replace("{count}", String.valueOf(ipTracker.getAccountCount(player.getUniqueId())));

        for (String action : actions) {
            switch (action.toUpperCase()) {
                case "KICK":
                    String kickMessage = plugin.getConfigManager().getMessage("alt-kick");
                    player.kickPlayer(kickMessage.replace("{player}", player.getName()));
                    break;
                case "NOTIFY_ADMINS":
                    Bukkit.getOnlinePlayers().stream()
                        .filter(online -> online.hasPermission("bloodcrates.admin.notify"))
                        .forEach(online -> online.sendMessage(adminMessage));
                    break;
                case "FLAG_ONLY":
                    plugin.getLogger().warning("Potential alt detected for " + player.getName() + " (" + player.getUniqueId() + ")");
                    break;
                default:
                    plugin.getLogger().warning("Unknown anti-alt action configured: " + action);
                    break;
            }
        }
    }
}
