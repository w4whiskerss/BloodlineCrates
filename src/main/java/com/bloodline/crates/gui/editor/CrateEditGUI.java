package com.bloodline.crates.gui.editor;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.manager.GUIManager;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.CrateType;
import com.bloodline.crates.model.KeyMode;
import com.bloodline.crates.util.ItemBuilder;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

public class CrateEditGUI implements GUIManager.GUIHandler {
    private final BloodlineCrates plugin;
    private final Crate crate;
    private final Inventory inventory;

    public CrateEditGUI(BloodlineCrates plugin, Crate crate) {
        this.plugin = plugin;
        this.crate = crate;
        this.inventory = Bukkit.createInventory(null, 45, "\u00A78Edit: " + crate.getDisplayName());
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
            case 11 -> requestName(player);
            case 13 -> requestDescription(player);
            case 15 -> {
                crate.setType(crate.getType() == CrateType.SELECTABLE ? CrateType.RANDOM : CrateType.SELECTABLE);
                plugin.getConfigManager().saveCrate(crate);
                refresh();
            }
            case 22 -> plugin.getGuiManager().openCrateAdjustments(player, crate);
            case 24 -> requestCooldown(player);
            case 29 -> plugin.getGuiManager().openKeyEdit(player, crate, GUIManager.KeyEditReturn.CRATE_EDIT);
            case 31 -> toggleKeyMode();
            case 33 -> plugin.getGuiManager().openRewardEdit(player, crate);
            case 40 -> plugin.getGuiManager().openCrateManager(player);
            default -> {
            }
        }
    }

    private void requestName(Player player) {
        plugin.getGuiManager().getInputHandler().requestInput(player, plugin.getConfigManager().getPrefix() + "\u00A7eType the crate name:", input ->
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!input.equalsIgnoreCase("cancel")) {
                    crate.setDisplayName(ChatColor.translateAlternateColorCodes('&', input));
                    plugin.getConfigManager().saveCrate(crate);
                }
                plugin.getGuiManager().openCrateEdit(player, crate);
            }));
    }

    private void requestDescription(Player player) {
        plugin.getGuiManager().getInputHandler().requestInput(player, plugin.getConfigManager().getPrefix() + "\u00A7eType the crate description:", input ->
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!input.equalsIgnoreCase("cancel")) {
                    crate.setDescription(input);
                    plugin.getConfigManager().saveCrate(crate);
                }
                plugin.getGuiManager().openCrateEdit(player, crate);
            }));
    }

    private void requestCooldown(Player player) {
        plugin.getGuiManager().getInputHandler().requestInput(player, plugin.getConfigManager().getPrefix() + "\u00A7eType the cooldown in seconds (0 to disable):", input ->
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!input.equalsIgnoreCase("cancel")) {
                    try {
                        long seconds = Long.parseLong(input.trim());
                        crate.setCooldownDurationSeconds(Math.max(0L, seconds));
                        crate.setCooldownEnabled(seconds > 0L);
                        plugin.getConfigManager().saveCrate(crate);
                    } catch (NumberFormatException exception) {
                        player.sendMessage(plugin.getConfigManager().getPrefix() + "\u00A7cInvalid number. Use seconds like 3600.");
                    }
                }
                plugin.getGuiManager().openCrateEdit(player, crate);
            }));
    }

    private void toggleKeyMode() {
        crate.setKeyMode(crate.getKeyMode() == KeyMode.VIRTUAL ? KeyMode.PHYSICAL : KeyMode.VIRTUAL);
        plugin.getConfigManager().saveCrate(crate);
        refresh();
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        populate();
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
    }

    public void refresh() {
        populate();
    }

    private void populate() {
        inventory.clear();
        inventory.setItem(11, new ItemBuilder(XMaterial.NAME_TAG).name("&eEdit Name").lore("&7Current: &f" + crate.getDisplayName()).build());
        inventory.setItem(13, new ItemBuilder(XMaterial.CHEST).name(crate.getDisplayName()).lore(crate.getDescriptionLines()).build());
        inventory.setItem(15, new ItemBuilder(crate.getType() == CrateType.SELECTABLE ? XMaterial.DIAMOND : XMaterial.REDSTONE)
            .name("&eToggle Type")
            .lore("&7Current: &f" + crate.getType().name())
            .build());
        inventory.setItem(22, new ItemBuilder(XMaterial.COMPARATOR)
            .name("&bAdjustments")
            .lore(
                "&7Rows: &f" + crate.getGuiRowsClamped(),
                "&7Reward Slots: &f" + crate.getEffectiveRewardSlots().size(),
                "&7Customize preview/select layout"
            )
            .build());
        inventory.setItem(24, new ItemBuilder(XMaterial.CLOCK)
            .name("&bEdit Cooldown")
            .lore(
                "&7Enabled: &f" + (crate.isCooldownEnabled() ? "Yes" : "No"),
                "&7Seconds: &f" + crate.getCooldownDurationSeconds(),
                "&7Uses chat input"
            )
            .build());
        inventory.setItem(29, new ItemBuilder(XMaterial.TRIPWIRE_HOOK).name("&aEdit Key").lore("&7Customize key name and item").build());
        inventory.setItem(31, new ItemBuilder(crate.getKeyMode() == KeyMode.VIRTUAL ? XMaterial.ENDER_PEARL : XMaterial.IRON_BARS)
            .name("&bToggle Key Mode")
            .lore(
                "&7Current: &f" + crate.getKeyMode().name(),
                "&7Physical: opens with item keys only",
                "&7Virtual: opens with stored keys only"
            )
            .build());
        inventory.setItem(33, new ItemBuilder(XMaterial.EMERALD).name("&aEdit Rewards").lore("&7Manage reward entries").build());
        inventory.setItem(40, new ItemBuilder(XMaterial.ARROW).name("&cBack").build());
        EditorUi.fill(inventory);
    }
}
