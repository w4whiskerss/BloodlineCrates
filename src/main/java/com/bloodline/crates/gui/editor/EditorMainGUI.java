package com.bloodline.crates.gui.editor;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.manager.GUIManager;
import com.bloodline.crates.util.ItemBuilder;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

public class EditorMainGUI implements GUIManager.GUIHandler {
    private final BloodlineCrates plugin;
    private final Inventory inventory;

    public EditorMainGUI(BloodlineCrates plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(null, 27, "§8Crate Editor");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() >= inventory.getSize()) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (event.getRawSlot() == 11) {
            plugin.getGuiManager().openCrateManager(player);
        } else if (event.getRawSlot() == 15) {
            plugin.getGuiManager().openKeyBrowser(player);
        }
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        inventory.clear();
        inventory.setItem(11, new ItemBuilder(XMaterial.CHEST).name("&6&lManage Crates").lore("&7Open crate editor").build());
        inventory.setItem(15, new ItemBuilder(XMaterial.TRIPWIRE_HOOK).name("&a&lManage Keys").lore("&7Open key editor").build());
        EditorUi.fill(inventory);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
    }
}
