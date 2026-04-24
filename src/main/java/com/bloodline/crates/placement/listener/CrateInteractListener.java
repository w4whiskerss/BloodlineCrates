package com.bloodline.crates.placement.listener;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.placement.PlacedCrate;
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

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        Player player = event.getPlayer();
        java.util.Optional<PlacedCrate> placedCrate = plugin.getPlacementManager().getCrateAt(clickedBlock.getLocation());
        if (placedCrate.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        Crate crate = plugin.getConfigManager().getCrate(placedCrate.get().getCrateId()).orElse(null);
        if (crate == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cThis placed crate points to a missing crate config.");
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            plugin.getGuiManager().openPreview(player, crate);
            return;
        }

        plugin.getCrateManager().handleCrateInteraction(player, clickedBlock.getLocation());
    }
}
