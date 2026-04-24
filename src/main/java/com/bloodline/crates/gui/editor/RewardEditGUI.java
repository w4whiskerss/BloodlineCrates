package com.bloodline.crates.gui.editor;

import com.bloodline.crates.BloodlineCrates;
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

public class RewardEditGUI implements GUIManager.GUIHandler {
    private final BloodlineCrates plugin;
    private final Crate crate;
    private final Inventory inventory;

    public RewardEditGUI(BloodlineCrates plugin, Crate crate) {
        this.plugin = plugin;
        this.crate = crate;
        this.inventory = Bukkit.createInventory(null, 54, "§8Edit Rewards");
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

        if (slot == 48) {
            plugin.getGuiManager().openCrateEdit(player, crate);
            return;
        }

        if (slot == 50) {
            ItemStack cursor = event.getCursor();
            if (cursor == null || cursor.getType().isAir()) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§cPick up an item from your inventory and click the green pane.");
                return;
            }
            plugin.getGuiManager().openRewardTypeSelector(player, crate, cursor.clone());
            return;
        }

        if (slot >= 0 && slot < crate.getRewards().size()) {
            Reward reward = crate.getRewards().get(slot);
            if (event.isShiftClick()) {
                plugin.getGuiManager().openConfirmDelete(player, crate, reward, slot, this);
            } else {
                plugin.getGuiManager().openRewardSettings(player, crate, reward, slot);
            }
        }
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        populate();
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
    }

    private void populate() {
        inventory.clear();
        for (int i = 0; i < crate.getRewards().size() && i < 45; i++) {
            Reward reward = crate.getRewards().get(i);
            ItemStack icon = reward.getDisplayItem();
            if (icon == null) {
                icon = new ItemBuilder(XMaterial.BARRIER).name("&cInvalid Reward").build();
            }
            inventory.setItem(i, new ItemBuilder(icon)
                .name(reward.getDisplayNameOrFallback())
                .addLore(
                    "",
                    "&7Type: &e" + reward.getRewardType().name(),
                    "&7Chance: &e" + String.format("%.2f", reward.getChance()) + "%",
                    "&7Permission: &e" + (reward.hasPermission() ? reward.getRequiredPermission() : "None"),
                    "",
                    "&eClick &7to edit",
                    "&eShift-Click &7to delete"
                )
                .build());
        }
        inventory.setItem(48, new ItemBuilder(XMaterial.ARROW).name("&cBack").build());
        inventory.setItem(50, new ItemBuilder(XMaterial.LIME_STAINED_GLASS_PANE)
            .name("&aAdd Reward")
            .lore("&7Hold an item on your cursor", "&7and click to create a reward")
            .build());
        EditorUi.fill(inventory);
    }

    public void removeReward(int index) {
        if (index >= 0 && index < crate.getRewards().size()) {
            crate.getRewards().remove(index);
            plugin.getConfigManager().saveCrate(crate);
            populate();
        }
    }
}
