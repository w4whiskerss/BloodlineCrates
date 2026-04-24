package com.bloodline.crates.util;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class InputHandler {
    private final BloodlineCrates plugin;
    private final Map<UUID, PendingInput> pendingInputs = new HashMap<>();
    private final Map<UUID, PendingAnvilInput> pendingAnvilInputs = new HashMap<>();

    public InputHandler(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    public void requestInput(Player player, String prompt, Consumer<String> callback) {
        player.closeInventory();
        player.sendMessage(prompt);
        player.sendMessage("\u00A77Type your response or '\u00A7ccancel\u00A77' to abort");
        pendingInputs.put(player.getUniqueId(), new PendingInput(callback));
    }

    public void requestAnvilInput(Player player, String title, String initialText, Consumer<String> callback) {
        cancelAnvilInput(player.getUniqueId());

        AnvilView view = MenuType.ANVIL.create(player, title);
        view.getTopInventory().setItem(0, createPaper(initialText));
        pendingAnvilInputs.put(player.getUniqueId(), new PendingAnvilInput(view, callback));
        player.openInventory(view);
    }

    public boolean hasPendingInput(UUID playerId) {
        return pendingInputs.containsKey(playerId);
    }

    public boolean hasPendingAnvilInput(UUID playerId) {
        return pendingAnvilInputs.containsKey(playerId);
    }

    public void handleInput(UUID playerId, String input) {
        PendingInput pending = pendingInputs.remove(playerId);
        if (pending != null) {
            pending.callback.accept(input);
        }
    }

    public void cancelInput(UUID playerId) {
        pendingInputs.remove(playerId);
    }

    public void handleAnvilPrepare(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }

        PendingAnvilInput pending = pendingAnvilInputs.get(player.getUniqueId());
        if (pending == null || pending.view != event.getView()) {
            return;
        }

        String text = resolveRenameText(event.getView(), pending.lastText);
        pending.lastText = text;
        event.setResult(createPaper(text));
    }

    public boolean handleAnvilClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return false;
        }

        PendingAnvilInput pending = pendingAnvilInputs.get(player.getUniqueId());
        if (pending == null || event.getView() != pending.view) {
            return false;
        }

        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return false;
        }

        event.setCancelled(true);
        if (event.getRawSlot() != 2) {
            return true;
        }

        String value = "";
        ItemStack result = event.getCurrentItem();
        if (result != null && result.hasItemMeta() && result.getItemMeta().hasDisplayName()) {
            value = result.getItemMeta().getDisplayName();
        }
        if (value == null || value.isBlank()) {
            value = resolveRenameText(event.getView(), pending.lastText);
        }

        event.setCurrentItem(null);
        event.getView().setItem(2, null);
        event.getWhoClicked().setItemOnCursor(null);
        pendingAnvilInputs.remove(player.getUniqueId());
        String resolved = value == null ? "" : value;
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.setItemOnCursor(null);
            player.updateInventory();
            player.closeInventory();
            pending.callback.accept(resolved);
        });
        return true;
    }

    public void handleAnvilClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        PendingAnvilInput pending = pendingAnvilInputs.get(player.getUniqueId());
        if (pending != null && pending.view == event.getView()) {
            pendingAnvilInputs.remove(player.getUniqueId());
        }
    }

    public void cancelAnvilInput(UUID playerId) {
        pendingAnvilInputs.remove(playerId);
    }

    private ItemStack createPaper(String text) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(text == null || text.isBlank() ? "0" : text);
            paper.setItemMeta(meta);
        }
        return paper;
    }

    private String resolveRenameText(org.bukkit.inventory.InventoryView view, String fallback) {
        if (view instanceof AnvilView anvilView) {
            String text = anvilView.getRenameText();
            if (text != null && !text.isBlank()) {
                return text;
            }
        }

        Inventory inventory = view.getTopInventory();
        ItemStack left = inventory.getItem(0);
        if (left != null && left.hasItemMeta() && left.getItemMeta().hasDisplayName()) {
            return left.getItemMeta().getDisplayName();
        }
        return (fallback == null || fallback.isBlank()) ? "0" : fallback;
    }

    private static class PendingInput {
        private final Consumer<String> callback;

        private PendingInput(Consumer<String> callback) {
            this.callback = callback;
        }
    }

    private static class PendingAnvilInput {
        private final AnvilView view;
        private final Consumer<String> callback;
        private String lastText;

        private PendingAnvilInput(AnvilView view, Consumer<String> callback) {
            this.view = view;
            this.callback = callback;
            this.lastText = "0";
        }
    }
}
