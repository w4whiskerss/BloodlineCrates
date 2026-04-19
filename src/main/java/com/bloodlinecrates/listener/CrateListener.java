package com.bloodlinecrates.listener;

import com.bloodlinecrates.BloodlineCratesPlugin;
import com.bloodlinecrates.model.PlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class CrateListener implements Listener {
    private final BloodlineCratesPlugin plugin;

    public CrateListener(BloodlineCratesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerProfile profile = plugin.playerDataManager().getOrLoad(event.getPlayer().getUniqueId());
        if (event.getPlayer().getAddress() != null) {
            profile.setLastKnownAddress(event.getPlayer().getAddress().getAddress().getHostAddress());
        }
        plugin.playerDataManager().save(profile);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.playerDataManager().save(plugin.playerDataManager().getOrLoad(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        Player player = event.getPlayer();
        plugin.crateManager().findByLocation(event.getClickedBlock().getLocation().add(0.5D, 0.0D, 0.5D))
                .ifPresent(crate -> {
                    event.setCancelled(true);
                    plugin.crateManager().open(player, crate, false, 1);
                });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        plugin.guiManager().handleClick(event);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        plugin.guiManager().handleClose(event);
    }
}
