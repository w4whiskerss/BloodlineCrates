package com.bloodline.crates.gui;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.gui.editor.EditorUi;
import com.bloodline.crates.manager.GUIManager;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.Reward;
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

public class PreviewGUI implements GUIManager.GUIHandler {
    private final BloodlineCrates plugin;
    private final Crate crate;
    private final Inventory inventory;

    public PreviewGUI(BloodlineCrates plugin, Crate crate) {
        this.plugin = plugin;
        this.crate = crate;
        this.inventory = Bukkit.createInventory(null, crate.getGuiRowsClamped() * 9, crate.getDisplayName() + " §8- Preview");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() != crate.getCloseButtonSlot()) {
            return;
        }
        event.getWhoClicked().closeInventory();
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        populateInventory(player);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
    }

    private void populateInventory(Player player) {
        inventory.clear();

        List<Integer> rewardSlots = crate.getEffectiveRewardSlots();
        int rewardIndex = 0;

        for (Reward reward : crate.getRewards()) {
            if (rewardIndex >= rewardSlots.size()) {
                break;
            }

            if (reward.hasPermission() && !player.hasPermission(reward.getRequiredPermission())) {
                continue;
            }

            ItemStack displayItem = reward.getDisplayItem();
            ItemBuilder builder = new ItemBuilder(displayItem);

            List<String> lore = new ArrayList<>();
            if (displayItem.hasItemMeta() && displayItem.getItemMeta().hasLore()) {
                lore.addAll(displayItem.getItemMeta().getLore());
            }
            lore.add("");
            lore.add("&7Type: &e" + reward.getRewardType().name());
            lore.add("&7Chance: &e" + String.format("%.2f", reward.getChance()) + "%");

            if (reward.hasPermission()) {
                lore.add("&7Permission: &c" + reward.getRequiredPermission());
            }

            builder.lore(lore);
            inventory.setItem(rewardSlots.get(rewardIndex), builder.build());
            rewardIndex++;
        }

        inventory.setItem(crate.getCloseButtonSlot(), new ItemBuilder(XMaterial.BARRIER).name("&cClose").build());
        EditorUi.fill(inventory);
    }
}
