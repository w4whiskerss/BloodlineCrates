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

import java.util.ArrayList;
import java.util.List;

public class KeyBrowserGUI implements GUIManager.GUIHandler {
    private final BloodlineCrates plugin;
    private final Inventory inventory;
    private final List<Crate> crates = new ArrayList<>();

    public KeyBrowserGUI(BloodlineCrates plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(null, 54, "§8Key Manager");
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
        int slot = event.getRawSlot();
        if (slot == 49) {
            plugin.getGuiManager().openEditorMain(player);
            return;
        }
        if (slot >= 0 && slot < crates.size()) {
            plugin.getGuiManager().openKeyEdit(player, crates.get(slot), GUIManager.KeyEditReturn.KEY_BROWSER);
        }
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        inventory.clear();
        crates.clear();
        crates.addAll(plugin.getConfigManager().getAllCrates());
        for (int i = 0; i < crates.size() && i < 45; i++) {
            Crate crate = crates.get(i);
            inventory.setItem(i, new ItemBuilder(crate.getKeyDisplayItem().clone())
                .name("&a" + crate.getDisplayName())
                .addLore("", "&7Edit the key for this crate")
                .build());
        }
        inventory.setItem(49, new ItemBuilder(XMaterial.ARROW).name("&cBack").build());
        EditorUi.fill(inventory);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
    }
}
