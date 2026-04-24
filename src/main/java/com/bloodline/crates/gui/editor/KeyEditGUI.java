package com.bloodline.crates.gui.editor;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.manager.GUIManager;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.util.ItemBuilder;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class KeyEditGUI implements GUIManager.GUIHandler {
    private final BloodlineCrates plugin;
    private final Crate crate;
    private final GUIManager.KeyEditReturn returnTarget;
    private final Inventory inventory;

    public KeyEditGUI(BloodlineCrates plugin, Crate crate, GUIManager.KeyEditReturn returnTarget) {
        this.plugin = plugin;
        this.crate = crate;
        this.returnTarget = returnTarget;
        this.inventory = Bukkit.createInventory(null, 45, "\u00A78Edit Key");
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
        if (slot == 11) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                crate.setKeyDisplayItem(cursor.clone());
                plugin.getConfigManager().saveCrate(crate);
                refresh();
            } else {
                player.sendMessage(plugin.getConfigManager().getPrefix() + "\u00A7cHold an item on your cursor and click to use it.");
            }
            return;
        }

        if (slot == 15) {
            plugin.getGuiManager().getInputHandler().requestInput(player, plugin.getConfigManager().getPrefix() + "\u00A7eType the key name:", input ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!input.equalsIgnoreCase("cancel")) {
                        ItemStack item = crate.getKeyDisplayItem().clone();
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', input));
                            item.setItemMeta(meta);
                        }
                        crate.setKeyDisplayItem(item);
                        plugin.getConfigManager().saveCrate(crate);
                    }
                    plugin.getGuiManager().openKeyEdit(player, crate, returnTarget);
                }));
            return;
        }

        if (slot == 31) {
            plugin.getGuiManager().getInputHandler().requestInput(player, plugin.getConfigManager().getPrefix() + "\u00A7eType the key description:", input ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!input.equalsIgnoreCase("cancel")) {
                        ItemStack item = crate.getKeyDisplayItem().clone();
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.setLore(Collections.singletonList(ChatColor.translateAlternateColorCodes('&', input)));
                            item.setItemMeta(meta);
                        }
                        crate.setKeyDisplayItem(item);
                        plugin.getConfigManager().saveCrate(crate);
                    }
                    plugin.getGuiManager().openKeyEdit(player, crate, returnTarget);
                }));
            return;
        }

        if (slot == 40) {
            if (returnTarget == GUIManager.KeyEditReturn.CRATE_EDIT) {
                plugin.getGuiManager().openCrateEdit(player, crate);
            } else {
                plugin.getGuiManager().openKeyBrowser(player);
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
        inventory.setItem(13, new ItemBuilder(crate.getKeyDisplayItem().clone()).name("&fKey Preview").build());
        inventory.setItem(11, new ItemBuilder(XMaterial.LIME_STAINED_GLASS_PANE).name("&aSet Key Item").lore("&7Hold an item on cursor and click").build());
        inventory.setItem(15, new ItemBuilder(XMaterial.NAME_TAG).name("&eEdit Key Name").build());
        inventory.setItem(29, new ItemBuilder(XMaterial.BOOK).name("&7Key Info").lore("&7This key keeps the exact", "&7item meta and NBT you use.").build());
        inventory.setItem(31, new ItemBuilder(XMaterial.PAPER).name("&eEdit Key Description").build());
        inventory.setItem(40, new ItemBuilder(XMaterial.ARROW).name("&cBack").build());
        EditorUi.fill(inventory);
    }

    public void refresh() {
        populate();
    }
}
