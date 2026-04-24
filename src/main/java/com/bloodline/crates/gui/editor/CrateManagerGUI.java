package com.bloodline.crates.gui.editor;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.manager.GUIManager;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.util.ItemBuilder;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class CrateManagerGUI implements GUIManager.GUIHandler {
    private final BloodlineCrates plugin;
    private final Inventory inventory;
    private final List<Crate> crateList = new ArrayList<>();
    
    public CrateManagerGUI(BloodlineCrates plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(null, 54, "§8Crate Manager");
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= inventory.getSize()) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        try {
            if (slot == 49) {
                plugin.getGuiManager().openEditorMain(player);
                return;
            }
            
            if (slot >= 0 && slot < crateList.size()) {
                Crate crate = crateList.get(slot);
                
                if (event.isShiftClick()) {
                    plugin.getGuiManager().openConfirmDelete(player, crate, this);
                } else {
                    plugin.getGuiManager().openCrateEdit(player, crate);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in CrateManagerGUI click: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void onOpen(InventoryOpenEvent event) {
        populateInventory();
    }
    
    @Override
    public void onClose(InventoryCloseEvent event) {
    }
    
    private void populateInventory() {
        inventory.clear();
        crateList.clear();
        crateList.addAll(plugin.getConfigManager().getAllCrates());
        
        int slot = 0;
        for (Crate crate : crateList) {
            if (slot >= 45) break;
            
            ItemStack icon = new ItemBuilder(XMaterial.CHEST)
                .name("&6" + crate.getDisplayName())
                .lore(
                    "&7ID: &e" + crate.getId(),
                    "&7Type: &e" + crate.getType().name(),
                    "&7Key: &e" + crate.getKeyId(),
                    "&7Rewards: &e" + crate.getRewards().size(),
                    "",
                    "&eLeft-Click &7to edit",
                    "&eShift-Click &7to delete"
                )
                .build();
            
            inventory.setItem(slot, icon);
            slot++;
        }
        
        inventory.setItem(49, new ItemBuilder(XMaterial.ARROW)
            .name("&cBack")
            .build());
        EditorUi.fill(inventory);
    }
    
    public void refresh() {
        populateInventory();
    }
}
