package com.bloodline.crates.placement.listener;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Iterator;

public class CrateProtectListener implements Listener {
    private final BloodlineCrates plugin;

    public CrateProtectListener(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) {
            return;
        }

        String crateId = null;
        for (String line : meta.getLore()) {
            String stripped = org.bukkit.ChatColor.stripColor(line);
            if (stripped != null && stripped.startsWith("Crate ID:")) {
                crateId = stripped.replace("Crate ID:", "").trim();
                break;
            }
        }

        if (crateId == null || plugin.getConfigManager().getCrate(crateId).isEmpty()) {
            return;
        }

        try {
            plugin.getPlacementManager().placeCrate(event.getPlayer(), crateId, event.getBlockPlaced().getLocation());
            event.getPlayer().sendMessage(plugin.getConfigManager().getPrefix() + "§aCrate placed successfully!");
        } catch (Exception exception) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getConfigManager().getPrefix() + "§c" + exception.getMessage());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (plugin.getPlacementManager().getCrateAt(block.getLocation()).isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("bloodcrates.admin.breakcrate")) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cYou cannot break placed crate blocks!");
            return;
        }

        plugin.getPlacementManager().removeCrate(block.getLocation());
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§aCrate removed.");
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        filterCrateBlocks(event.blockList().iterator());
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        filterCrateBlocks(event.blockList().iterator());
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (plugin.getPlacementManager().getCrateAt(event.getBlock().getLocation()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> plugin.getPlacementManager().getCrateAt(block.getLocation()).isPresent())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> plugin.getPlacementManager().getCrateAt(block.getLocation()).isPresent())) {
            event.setCancelled(true);
        }
    }

    private void filterCrateBlocks(Iterator<Block> iterator) {
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (plugin.getPlacementManager().getCrateAt(block.getLocation()).isPresent()) {
                iterator.remove();
            }
        }
    }
}
