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

public class CrateLayoutEditorGUI implements GUIManager.GUIHandler {
    private final BloodlineCrates plugin;
    private final Crate crate;
    private final Inventory inventory;

    public CrateLayoutEditorGUI(BloodlineCrates plugin, Crate crate) {
        this.plugin = plugin;
        this.crate = crate;
        this.inventory = Bukkit.createInventory(null, crate.getGuiRowsClamped() * 9, "§8Reward Slots");
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() >= inventory.getSize()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int closeSlot = crate.getCloseButtonSlot();
        int slot = event.getRawSlot();

        if (slot == closeSlot) {
            plugin.getConfigManager().saveCrate(crate);
            plugin.getGuiManager().openCrateAdjustments(player, crate);
            return;
        }

        List<Integer> rewardSlots = new ArrayList<>(crate.getEffectiveRewardSlots());
        if (rewardSlots.contains(slot)) {
            rewardSlots.remove(Integer.valueOf(slot));
        } else {
            rewardSlots.add(slot);
            rewardSlots.sort(Integer::compareTo);
        }

        crate.setRewardSlots(rewardSlots);
        plugin.getConfigManager().saveCrate(crate);
        populate();
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        populate();
    }

    private void populate() {
        inventory.clear();
        List<Integer> rewardSlots = crate.getEffectiveRewardSlots();
        int closeSlot = crate.getCloseButtonSlot();

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (slot == closeSlot) {
                inventory.setItem(slot, new ItemBuilder(XMaterial.ARROW)
                    .name("&cBack")
                    .lore("&7Save layout and return")
                    .build());
                continue;
            }

            boolean enabled = rewardSlots.contains(slot);
            inventory.setItem(slot, new ItemBuilder(enabled ? XMaterial.LIME_STAINED_GLASS_PANE : XMaterial.RED_STAINED_GLASS_PANE)
                .name(enabled ? "&aReward Slot" : "&cUnused Slot")
                .lore(
                    "&7Slot: &f" + slot,
                    enabled ? "&7Click to disable this position" : "&7Click to enable this position"
                )
                .build());
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
