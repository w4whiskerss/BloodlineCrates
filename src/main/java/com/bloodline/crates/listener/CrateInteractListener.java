package com.bloodline.crates.listener;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class CrateInteractListener implements Listener {
    private final BloodlineCrates plugin;
    
    public CrateInteractListener(BloodlineCrates plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        
        Player player = event.getPlayer();
        
        plugin.getCrateManager().getCrateIdAtLocation(block.getLocation()).ifPresent(crateId -> {
            event.setCancelled(true);
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                plugin.getConfigManager().getCrate(crateId)
                    .ifPresent(crate -> plugin.getGuiManager().openPreview(player, crate));
                return;
            }

            plugin.getCrateManager().handleCrateInteraction(player, block.getLocation());
        });
    }
}
