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

public class ConfirmDeleteGUI implements GUIManager.GUIHandler {
    private final BloodlineCrates plugin;
    private final Inventory inventory;
    private final DeleteType deleteType;
    private final Crate crate;
    private final int rewardIndex;
    private final GUIManager.GUIHandler returnGUI;

    public ConfirmDeleteGUI(BloodlineCrates plugin, Crate crate, GUIManager.GUIHandler returnGUI) {
        this.plugin = plugin;
        this.crate = crate;
        this.rewardIndex = -1;
        this.returnGUI = returnGUI;
        this.deleteType = DeleteType.CRATE;
        this.inventory = Bukkit.createInventory(null, 27, "§cConfirm Delete");
    }

    public ConfirmDeleteGUI(BloodlineCrates plugin, Crate crate, Reward reward, int rewardIndex, GUIManager.GUIHandler returnGUI) {
        this.plugin = plugin;
        this.crate = crate;
        this.rewardIndex = rewardIndex;
        this.returnGUI = returnGUI;
        this.deleteType = DeleteType.REWARD;
        this.inventory = Bukkit.createInventory(null, 27, "§cConfirm Delete");
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

        try {
            if (slot == 11) {
                handleConfirm(player);
            } else if (slot == 15) {
                handleCancel(player);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in ConfirmDeleteGUI click: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleConfirm(Player player) {
        if (deleteType == DeleteType.CRATE) {
            plugin.getConfigManager().deleteCrate(crate.getId());
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§aCrate deleted: " + crate.getDisplayName());

            if (returnGUI instanceof CrateManagerGUI crateManagerGUI) {
                crateManagerGUI.refresh();
                plugin.getGuiManager().reopenExistingGUI(player, returnGUI, GUIManager.GUIType.CRATE_MANAGER);
            } else {
                plugin.getGuiManager().openCrateManager(player);
            }
        } else if (deleteType == DeleteType.REWARD && returnGUI instanceof RewardEditGUI rewardEditGUI) {
            rewardEditGUI.removeReward(rewardIndex);
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§aReward deleted!");
            plugin.getGuiManager().reopenExistingGUI(player, returnGUI, GUIManager.GUIType.REWARD_EDIT);
        }
    }

    private void handleCancel(Player player) {
        plugin.getGuiManager().reopenExistingGUI(player, returnGUI, resolveReturnType());
    }

    private GUIManager.GUIType resolveReturnType() {
        if (returnGUI instanceof RewardEditGUI) {
            return GUIManager.GUIType.REWARD_EDIT;
        }
        if (returnGUI instanceof CrateManagerGUI) {
            return GUIManager.GUIType.CRATE_MANAGER;
        }
        if (returnGUI instanceof CrateEditGUI) {
            return GUIManager.GUIType.CRATE_EDIT;
        }
        if (returnGUI instanceof KeyEditGUI) {
            return GUIManager.GUIType.KEY_EDIT;
        }
        return GUIManager.GUIType.EDITOR_MAIN;
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

        String targetName = deleteType == DeleteType.CRATE ? crate.getDisplayName() : "Reward";

        inventory.setItem(13, new ItemBuilder(XMaterial.BARRIER)
            .name("§c§lDELETE " + targetName.toUpperCase())
            .lore(
                "&7Are you sure you want to",
                "&7delete this " + (deleteType == DeleteType.CRATE ? "crate" : "reward") + "?",
                "",
                "&c&lThis cannot be undone!"
            )
            .build());

        inventory.setItem(11, new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE)
            .name("&c&lCONFIRM DELETE")
            .lore("&7Click to confirm deletion")
            .build());

        inventory.setItem(15, new ItemBuilder(XMaterial.GREEN_STAINED_GLASS_PANE)
            .name("&a&lCANCEL")
            .lore("&7Click to go back")
            .build());
    }

    private enum DeleteType {
        CRATE,
        REWARD
    }
}
