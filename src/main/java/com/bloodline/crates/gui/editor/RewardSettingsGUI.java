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

public class RewardSettingsGUI implements GUIManager.GUIHandler {
    private final BloodlineCrates plugin;
    private final Crate crate;
    private final Reward reward;
    private final int rewardIndex;
    private final Inventory inventory;

    public RewardSettingsGUI(BloodlineCrates plugin, Crate crate, Reward reward, int rewardIndex) {
        this.plugin = plugin;
        this.crate = crate;
        this.reward = reward;
        this.rewardIndex = rewardIndex;
        this.inventory = Bukkit.createInventory(null, 45, "§8Reward Settings");
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
        switch (event.getRawSlot()) {
            case 10 -> ask(player, "§eType the reward name or 'none':", input -> reward.setDisplayName(input.equalsIgnoreCase("none") ? null : input));
            case 12 -> ask(player, "§eType the reward description or 'none':", input -> reward.setDescription(input.equalsIgnoreCase("none") ? null : input));
            case 14 -> ask(player, "§eType the chance (0-100):", input -> reward.setChance(Double.parseDouble(input)));
            case 16 -> ask(player, "§eType permission or 'none':", input -> reward.setRequiredPermission(input.equalsIgnoreCase("none") ? "" : input));
            case 31 -> {
                if (reward.isCommandReward()) {
                    ask(player, "§eType the command without /:", input -> reward.setCommand(input.trim().replaceFirst("^/+", "")));
                }
            }
            case 40 -> plugin.getGuiManager().openRewardEdit(player, crate);
            default -> {
            }
        }
    }

    private void ask(Player player, String prompt, java.util.function.Consumer<String> consumer) {
        plugin.getGuiManager().getInputHandler().requestInput(player, plugin.getConfigManager().getPrefix() + prompt, input ->
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.resetTitle();
                if (!input.equalsIgnoreCase("cancel")) {
                    try {
                        consumer.accept(input);
                        plugin.getConfigManager().saveCrate(crate);
                    } catch (Exception exception) {
                        player.sendMessage(plugin.getConfigManager().getPrefix() + "§cInvalid input.");
                    }
                }

                if (crate.getRewards().isEmpty()) {
                    plugin.getGuiManager().openRewardEdit(player, crate);
                    return;
                }

                Reward refreshed = crate.getRewards().get(Math.min(rewardIndex, crate.getRewards().size() - 1));
                plugin.getGuiManager().openRewardSettings(player, crate, refreshed, Math.min(rewardIndex, crate.getRewards().size() - 1));
            }));
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        inventory.clear();
        inventory.setItem(10, new ItemBuilder(XMaterial.NAME_TAG)
            .name("&eEdit Name")
            .lore("&7Current: &f" + reward.getDisplayNameOrFallback())
            .build());
        inventory.setItem(12, new ItemBuilder(XMaterial.WRITABLE_BOOK)
            .name("&eEdit Description")
            .lore("&7Current: &f" + (reward.getDescription() == null || reward.getDescription().isBlank() ? "None" : reward.getDescription()))
            .build());
        inventory.setItem(14, new ItemBuilder(XMaterial.PAPER)
            .name("&eEdit Chance")
            .lore("&7Current: &f" + String.format("%.2f", reward.getChance()) + "%")
            .build());
        inventory.setItem(16, new ItemBuilder(XMaterial.OAK_SIGN)
            .name("&eEdit Permission")
            .lore("&7Current: &f" + (reward.hasPermission() ? reward.getRequiredPermission() : "None"))
            .build());
        inventory.setItem(22, new ItemBuilder(reward.getDisplayItem())
            .name(reward.getDisplayNameOrFallback())
            .lore(reward.getDescriptionLines())
            .build());
        inventory.setItem(31, new ItemBuilder(reward.isCommandReward() ? XMaterial.COMMAND_BLOCK : XMaterial.CHEST)
            .name(reward.isCommandReward() ? "&eEdit Command" : "&7Reward Type")
            .lore(reward.isCommandReward()
                ? "&7Current: &f" + (reward.getCommand() == null || reward.getCommand().isBlank() ? "None" : reward.getCommand())
                : "&7This reward gives the stored item", "&7exactly as shown.")
            .build());
        inventory.setItem(40, new ItemBuilder(XMaterial.ARROW).name("&cBack").build());
        EditorUi.fill(inventory);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
    }
}
