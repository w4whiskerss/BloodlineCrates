package com.bloodline.crates.gui;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.gui.editor.EditorUi;
import com.bloodline.crates.manager.GUIManager;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.KeyMode;
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

public class ConfirmGUI implements GUIManager.GUIHandler {
    private final BloodlineCrates plugin;
    private final Crate crate;
    private final Reward reward;
    private final Inventory inventory;
    private final Location crateLocation;
    private final boolean consumeVirtualCrate;

    public ConfirmGUI(BloodlineCrates plugin, Crate crate, Reward reward, Location crateLocation) {
        this(plugin, crate, reward, crateLocation, false);
    }

    public ConfirmGUI(BloodlineCrates plugin, Crate crate, Reward reward, Location crateLocation, boolean consumeVirtualCrate) {
        this.plugin = plugin;
        this.crate = crate;
        this.reward = reward;
        this.crateLocation = crateLocation;
        this.consumeVirtualCrate = consumeVirtualCrate;
        this.inventory = Bukkit.createInventory(null, 27, "§8Confirm Selection");
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

        if (slot == 11) {
            if (consumeVirtualCrate && !plugin.getVirtualInventoryManager().removeVirtualCrate(player.getUniqueId(), crate.getId(), 1)) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§cYou no longer have that virtual crate.");
                player.closeInventory();
                return;
            }

            if (!plugin.getKeyManager().consumeKey(player, crate)) {
                if (consumeVirtualCrate) {
                    plugin.getVirtualInventoryManager().addVirtualCrate(player.getUniqueId(), crate.getId(), 1);
                }

                player.sendMessage(plugin.getConfigManager().getPrefix()
                    + (crate.getKeyMode() == KeyMode.VIRTUAL
                    ? "§cYou no longer have a virtual key!"
                    : "§cYou no longer have a physical key!"));
                player.closeInventory();
                return;
            }

            player.closeInventory();
            Location animationLocation = crateLocation != null ? crateLocation : player.getLocation();
            plugin.getCrateManager().openSelectedCrate(player, crate, reward, animationLocation);
        } else if (slot == 15) {
            plugin.getGuiManager().openSelectable(player, crate, crateLocation, consumeVirtualCrate);
        }
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

        ItemStack rewardDisplay = new ItemBuilder(reward.getDisplayItem())
            .addLore("", consumeVirtualCrate ? "&7This will also consume one virtual crate." : "&eClick confirm to receive this reward!")
            .build();
        inventory.setItem(13, rewardDisplay);

        ItemStack confirmButton = new ItemBuilder(XMaterial.LIME_STAINED_GLASS_PANE)
            .name("&a&lCONFIRM")
            .lore("&7Click to confirm and receive", "&7this reward!")
            .build();
        inventory.setItem(11, confirmButton);

        ItemStack cancelButton = new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE)
            .name("&c&lCANCEL")
            .lore("&7Click to go back and", "&7choose a different reward")
            .build();
        inventory.setItem(15, cancelButton);
        EditorUi.fill(inventory);
    }
}
