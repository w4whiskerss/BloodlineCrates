package com.bloodline.crates.gui;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.gui.editor.EditorUi;
import com.bloodline.crates.manager.GUIManager;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.Reward;
import com.bloodline.crates.util.ItemBuilder;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SelectableGUI implements GUIManager.GUIHandler {
    private final BloodlineCrates plugin;
    private final Crate crate;
    private final Inventory inventory;
    private final List<Reward> eligibleRewards = new ArrayList<>();
    private final Location crateLocation;
    private final boolean consumeVirtualCrate;

    public SelectableGUI(BloodlineCrates plugin, Crate crate, Location crateLocation) {
        this(plugin, crate, crateLocation, false);
    }

    public SelectableGUI(BloodlineCrates plugin, Crate crate, Location crateLocation, boolean consumeVirtualCrate) {
        this.plugin = plugin;
        this.crate = crate;
        this.crateLocation = crateLocation;
        this.consumeVirtualCrate = consumeVirtualCrate;
        this.inventory = Bukkit.createInventory(null, crate.getGuiRowsClamped() * 9, crate.getDisplayName() + " §8- Select");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == crate.getCloseButtonSlot()) {
            player.closeInventory();
            return;
        }

        int rewardIndex = crate.getEffectiveRewardSlots().indexOf(slot);
        if (rewardIndex >= 0 && rewardIndex < eligibleRewards.size()) {
            Reward selectedReward = eligibleRewards.get(rewardIndex);
            plugin.getGuiManager().openConfirm(player, crate, selectedReward, crateLocation, consumeVirtualCrate);
        }
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
        eligibleRewards.clear();

        for (Reward reward : crate.getRewards()) {
            if (!reward.hasPermission() || player.hasPermission(reward.getRequiredPermission())) {
                eligibleRewards.add(reward);
            }
        }

        List<Integer> rewardSlots = crate.getEffectiveRewardSlots();
        for (int index = 0; index < eligibleRewards.size() && index < rewardSlots.size(); index++) {
            Reward reward = eligibleRewards.get(index);
            ItemStack displayItem = reward.getDisplayItem();
            ItemBuilder builder = new ItemBuilder(displayItem);

            List<String> lore = new ArrayList<>();
            if (displayItem.hasItemMeta() && displayItem.getItemMeta().hasLore()) {
                lore.addAll(displayItem.getItemMeta().getLore());
            }
            lore.add("");
            lore.add("&7Type: &e" + reward.getRewardType().name());
            lore.add("&eClick to select this reward!");

            builder.lore(lore);
            inventory.setItem(rewardSlots.get(index), builder.build());
        }

        inventory.setItem(crate.getCloseButtonSlot(), new ItemBuilder(XMaterial.BARRIER).name("&cClose").build());
        EditorUi.fill(inventory);
    }
}
