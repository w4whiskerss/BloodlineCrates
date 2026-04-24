package com.bloodline.crates.gui;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.gui.editor.EditorUi;
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

public class VirtualOpenConfirmGUI implements GUIManager.GUIHandler {
    private final BloodlineCrates plugin;
    private final Crate crate;
    private final Inventory inventory;

    public VirtualOpenConfirmGUI(BloodlineCrates plugin, Crate crate) {
        this.plugin = plugin;
        this.crate = crate;
        this.inventory = Bukkit.createInventory(null, 27, "§8Confirm Virtual Open");
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 11) {
            if (!plugin.getCrateManager().canOpen(player, crate)) {
                player.closeInventory();
                return;
            }
            if (!plugin.getVirtualInventoryManager().removeVirtualCrate(player.getUniqueId(), crate.getId(), 1)) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§cYou no longer have that virtual crate.");
                player.closeInventory();
                return;
            }
            player.closeInventory();
            plugin.getCrateManager().openRandomCrate(player, crate, null);
            return;
        }

        if (slot == 15) {
            plugin.getVirtualInventoryManager().openGUI(player);
        }
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        ItemStack crateIcon = plugin.getCrateManager().createCrateItem(crate, 1);
        inventory.clear();
        inventory.setItem(13, new ItemBuilder(crateIcon)
            .addLore("", "&7This will consume one virtual crate.", "&7Your key will still be required.")
            .build());
        inventory.setItem(11, new ItemBuilder(XMaterial.LIME_STAINED_GLASS_PANE)
            .name("&a&lOPEN")
            .lore("&7Consume one virtual crate", "&7and open this crate now")
            .build());
        inventory.setItem(15, new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE)
            .name("&c&lCANCEL")
            .lore("&7Go back to your virtual crate inventory")
            .build());
        EditorUi.fill(inventory);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
