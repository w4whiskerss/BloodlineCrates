package com.bloodline.crates.gui.editor;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.manager.GUIManager;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.Reward;
import com.bloodline.crates.reward.RewardType;
import com.bloodline.crates.util.ItemBuilder;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class RewardTypeSelectGUI implements GUIManager.GUIHandler {
    private final BloodlineCrates plugin;
    private final Crate crate;
    private final ItemStack baseItem;
    private final Inventory inventory;

    public RewardTypeSelectGUI(BloodlineCrates plugin, Crate crate, ItemStack baseItem) {
        this.plugin = plugin;
        this.crate = crate;
        this.baseItem = baseItem;
        this.inventory = Bukkit.createInventory(null, 27, "§8Reward Type");
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
            Reward reward = new Reward(nextRewardId(), RewardType.ITEM, baseItem.clone(), 10.0D, "", false, 0.0D, null, null, null);
            crate.getRewards().add(reward);
            plugin.getConfigManager().saveCrate(crate);
            plugin.getGuiManager().openRewardSettings(player, crate, reward, crate.getRewards().size() - 1);
            return;
        }

        if (event.getRawSlot() == 15) {
            player.closeInventory();
            player.sendTitle("§aCommand Reward", "§fType the command in chat without /", 10, 2000, 10);
            plugin.getGuiManager().getInputHandler().requestInput(player, plugin.getConfigManager().getPrefix() + "§eType the command reward:", input ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.resetTitle();
                    if (!input.equalsIgnoreCase("cancel")) {
                        Reward reward = new Reward(
                            nextRewardId(),
                            RewardType.COMMAND,
                            baseItem.clone(),
                            10.0D,
                            "",
                            false,
                            0.0D,
                            input.trim().replaceFirst("^/+", ""),
                            null,
                            null
                        );
                        crate.getRewards().add(reward);
                        plugin.getConfigManager().saveCrate(crate);
                        plugin.getGuiManager().openRewardSettings(player, crate, reward, crate.getRewards().size() - 1);
                        return;
                    }
                    plugin.getGuiManager().openRewardEdit(player, crate);
                }));
            return;
        }

        if (event.getRawSlot() == 22) {
            plugin.getGuiManager().openRewardEdit(player, crate);
        }
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        inventory.clear();
        inventory.setItem(11, new ItemBuilder(XMaterial.CHEST)
            .name("&aItem Reward")
            .lore("&7Give the exact dragged item", "&7with all meta and NBT intact")
            .build());
        inventory.setItem(13, new ItemBuilder(baseItem.clone()).name("&fDragged Item Preview").build());
        inventory.setItem(15, new ItemBuilder(XMaterial.COMMAND_BLOCK)
            .name("&eCommand Reward")
            .lore("&7Use the dragged item as icon", "&7then type the command in chat")
            .build());
        inventory.setItem(22, new ItemBuilder(XMaterial.ARROW).name("&cBack").build());
        EditorUi.fill(inventory);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
    }

    private String nextRewardId() {
        return crate.getId() + "_reward_" + System.currentTimeMillis();
    }
}
