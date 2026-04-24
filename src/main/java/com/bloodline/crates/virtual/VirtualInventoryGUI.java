package com.bloodline.crates.virtual;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.manager.GUIManager;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.util.HeadUtil;
import com.bloodline.crates.util.ItemBuilder;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class VirtualInventoryGUI implements GUIManager.GUIHandler {
    private static final int PAGE_SIZE = 45;
    private static final String PREV_HEAD = "bbf4f589ffda6ce9b14cb1d85dc6ff1342f3d7102b6670eecb5c8a3f44b5e4d";
    private static final String NEXT_HEAD = "63b9b6a50b7ed0f49d2e6b1e4b5d87e4d18f0d33bd44a14bcbb157ccd44b4d2";
    private static final String CLOSE_HEAD = "d34ef0634bb497f95eb9b14f3b1d2a84f4a5f0a50a2cf6930f33e32e3c2ac4";
    private static final String INFO_HEAD = "9c6a5d66c4b9a6c8a9c8eb7f0a46a9c3fba2d3e0fcbcd03df866f3af3c0d938";

    private final BloodlineCrates plugin;
    private final Player owner;
    private final boolean readOnly;
    private final Inventory inventory;
    private int page;
    private List<Entry> entries = List.of();

    public VirtualInventoryGUI(BloodlineCrates plugin, Player viewer, Player owner, boolean readOnly) {
        this.plugin = plugin;
        this.owner = owner;
        this.readOnly = readOnly;
        String title = ChatColor.translateAlternateColorCodes('&',
            (readOnly ? "[" + owner.getName() + "] " : "")
                + plugin.getConfig().getString("virtual-inventory.gui-title", "&8Virtual Crate Inventory"));
        this.inventory = Bukkit.createInventory(null, 54, title);
        this.page = 0;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() >= inventory.getSize()) {
            return;
        }

        Player viewer = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 45 && page > 0) {
            page--;
            populate();
            return;
        }
        if (slot == 53 && (page + 1) * PAGE_SIZE < entries.size()) {
            page++;
            populate();
            return;
        }
        if (slot == 49) {
            viewer.closeInventory();
            return;
        }

        if (slot >= PAGE_SIZE) {
            return;
        }

        int entryIndex = page * PAGE_SIZE + slot;
        if (entryIndex < 0 || entryIndex >= entries.size()) {
            return;
        }

        Entry entry = entries.get(entryIndex);
        if (event.getClick() == ClickType.RIGHT) {
            viewer.sendMessage(plugin.getConfigManager().getPrefix()
                + "§e" + entry.crate().getDisplayName() + " §7Owned: §f" + entry.amount());
            return;
        }

        if (readOnly) {
            viewer.sendMessage(plugin.getConfigManager().getPrefix() + "§cThis is a read-only virtual crate inventory.");
            return;
        }

        if (entry.crate().getType() == com.bloodline.crates.model.CrateType.SELECTABLE) {
            plugin.getGuiManager().openSelectable(viewer, entry.crate(), null, true);
            return;
        }

        plugin.getGuiManager().openVirtualOpenConfirm(viewer, entry.crate());
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        populate();
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void populate() {
        inventory.clear();
        entries = buildEntries();

        int start = page * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            Entry entry = entries.get(i);
            inventory.setItem(i - start, buildCrateIcon(entry));
        }

        ItemStack filler = createFillerItem();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler.clone());
            }
        }

        if (page > 0) {
            inventory.setItem(45, HeadUtil.createCustomHead(PREV_HEAD, XMaterial.ARROW, "&ePrevious Page", List.of("&7Go to the previous page")));
        }
        if ((page + 1) * PAGE_SIZE < entries.size()) {
            inventory.setItem(53, HeadUtil.createCustomHead(NEXT_HEAD, XMaterial.ARROW, "&eNext Page", List.of("&7Go to the next page")));
        }
        inventory.setItem(49, HeadUtil.createCustomHead(CLOSE_HEAD, XMaterial.BARRIER, readOnly ? "&cClose View" : "&cClose Inventory", List.of("&7Close this menu")));
        inventory.setItem(47, HeadUtil.createCustomHead(INFO_HEAD, XMaterial.BOOK, "&6Inventory Info", List.of(
            "&7Owner: &f" + owner.getName(),
            "&7Mode: &f" + (readOnly ? "Read-only" : "Interactive"),
            "&7Left-click: &fOpen",
            "&7Right-click: &fInfo only"
        )));
    }

    private List<Entry> buildEntries() {
        Map<String, Integer> virtualCrates = plugin.getVirtualInventoryManager().getAllVirtualCrates(owner.getUniqueId());
        List<Entry> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : virtualCrates.entrySet()) {
            Optional<Crate> crate = plugin.getConfigManager().getCrate(entry.getKey());
            if (crate.isPresent() && entry.getValue() > 0) {
                result.add(new Entry(crate.get(), entry.getValue()));
            }
        }
        result.sort(Comparator.comparing(entry -> ChatColor.stripColor(entry.crate().getDisplayName()), String.CASE_INSENSITIVE_ORDER));
        if (page > 0 && page * PAGE_SIZE >= result.size()) {
            page = Math.max(0, (result.size() - 1) / PAGE_SIZE);
        }
        return result;
    }

    private ItemStack buildCrateIcon(Entry entry) {
        ItemStack icon = plugin.getCrateManager().createCrateItem(entry.crate(), 1);
        if (icon == null) {
            icon = new ItemBuilder(XMaterial.CHEST).name(entry.crate().getDisplayName()).build();
        }

        ItemMeta meta = icon.getItemMeta();
        List<String> lore = meta != null && meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Owned: &e" + entry.amount()));
        lore.add(ChatColor.translateAlternateColorCodes('&', readOnly ? "&7Read-only admin view" : "&eLeft-click to open"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Right-click for info"));
        return new ItemBuilder(icon).lore(lore).build();
    }

    private ItemStack createFillerItem() {
        ItemStack filler = XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();
        if (filler == null) {
            filler = XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
        }
        filler = filler == null ? new ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE) : filler;

        String materialName = plugin.getConfig().getString("virtual-inventory.filler-item.material", "GRAY_STAINED_GLASS_PANE");
        ItemStack configured = XMaterial.matchXMaterial(materialName).map(XMaterial::parseItem).orElse(filler);
        if (configured == null) {
            configured = filler;
        }

        String name = plugin.getConfig().getString("virtual-inventory.filler-item.name", " ");
        return new ItemBuilder(configured).name(name).build();
    }

    private record Entry(Crate crate, int amount) {
    }
}
