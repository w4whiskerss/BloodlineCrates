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

public class CrateAdjustmentsGUI implements GUIManager.GUIHandler {
    private final BloodlineCrates plugin;
    private final Crate crate;
    private final Inventory inventory;

    public CrateAdjustmentsGUI(BloodlineCrates plugin, Crate crate) {
        this.plugin = plugin;
        this.crate = crate;
        this.inventory = Bukkit.createInventory(null, 27, "§8Adjustments: " + crate.getDisplayName());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() >= inventory.getSize()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        switch (event.getRawSlot()) {
            case 10 -> updateRows(player, crate.getGuiRowsClamped() - 1);
            case 13 -> plugin.getGuiManager().openCrateLayoutEditor(player, crate);
            case 16 -> updateRows(player, crate.getGuiRowsClamped() + 1);
            case 22 -> {
                crate.setRewardSlots(java.util.Collections.emptyList());
                plugin.getConfigManager().saveCrate(crate);
                plugin.getGuiManager().openCrateAdjustments(player, crate);
            }
            case 18 -> plugin.getGuiManager().openCrateEdit(player, crate);
            default -> {
            }
        }
    }

    private void updateRows(Player player, int rows) {
        crate.setGuiRows(Math.max(1, Math.min(6, rows)));
        crate.setRewardSlots(crate.getEffectiveRewardSlots().stream()
            .filter(slot -> slot < crate.getCloseButtonSlot())
            .toList());
        plugin.getConfigManager().saveCrate(crate);
        plugin.getGuiManager().openCrateAdjustments(player, crate);
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        inventory.clear();
        inventory.setItem(10, new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE)
            .name("&cLess Rows")
            .lore("&7Current: &f" + crate.getGuiRowsClamped())
            .build());
        inventory.setItem(13, new ItemBuilder(XMaterial.CRAFTING_TABLE)
            .name("&eEdit Reward Layout")
            .lore(
                "&7Rows: &f" + crate.getGuiRowsClamped(),
                "&7Reward Slots: &f" + crate.getEffectiveRewardSlots().size(),
                "",
                "&7Click to choose exact reward positions"
            )
            .build());
        inventory.setItem(16, new ItemBuilder(XMaterial.LIME_STAINED_GLASS_PANE)
            .name("&aMore Rows")
            .lore("&7Current: &f" + crate.getGuiRowsClamped())
            .build());
        inventory.setItem(22, new ItemBuilder(XMaterial.BARRIER)
            .name("&cReset Layout")
            .lore("&7Return to default slot order")
            .build());
        inventory.setItem(18, new ItemBuilder(XMaterial.ARROW).name("&cBack").build());
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
