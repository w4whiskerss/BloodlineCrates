package com.bloodline.crates.listener;

import com.bloodline.crates.BloodlineCrates;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Optional;

public class BlockListener implements Listener {
    private final BloodlineCrates plugin;
    
    public BlockListener(BloodlineCrates plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName() || !meta.hasLore()) {
            return;
        }
        
        String lore = String.join(" ", meta.getLore());
        if (!lore.contains("Crate ID:")) {
            return;
        }
        
        for (String line : meta.getLore()) {
            if (line.contains("Crate ID:")) {
                String crateId = line.replace("§7Crate ID: §e", "").replace("Crate ID: ", "").trim();
                
                if (plugin.getConfigManager().getCrate(crateId).isPresent()) {
                    Block block = event.getBlockPlaced();
                    plugin.getCrateManager().registerCrateLocation(block.getLocation(), crateId);
                    event.getPlayer().sendMessage(plugin.getConfigManager().getPrefix() + "§aCrate placed successfully!");
                }
                break;
            }
        }
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Optional<String> crateIdOpt = plugin.getCrateManager().getCrateIdAtLocation(block.getLocation());
        
        if (crateIdOpt.isPresent()) {
            Player player = event.getPlayer();
            
            if (!player.hasPermission("bloodcrates.admin.breakcrate")) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§cYou cannot break crate blocks!");
                return;
            }
            
            plugin.getCrateManager().unregisterCrateLocation(block.getLocation());
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§aCrate removed.");
        }
    }
}
