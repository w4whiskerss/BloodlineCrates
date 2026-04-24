package com.bloodline.crates.util;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemBuilder {
    private ItemStack item;
    private ItemMeta meta;
    
    public ItemBuilder(XMaterial material) {
        this.item = material.parseItem();
        if (this.item != null) {
            this.meta = this.item.getItemMeta();
        }
    }
    
    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }
    
    public ItemBuilder name(String name) {
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        }
        return this;
    }
    
    public ItemBuilder lore(String... lore) {
        if (meta != null) {
            List<String> coloredLore = Arrays.stream(lore)
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
            meta.setLore(coloredLore);
        }
        return this;
    }
    
    public ItemBuilder lore(List<String> lore) {
        if (meta != null) {
            List<String> coloredLore = lore.stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
            meta.setLore(coloredLore);
        }
        return this;
    }
    
    public ItemBuilder addLore(String... lines) {
        if (meta != null) {
            List<String> currentLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            for (String line : lines) {
                currentLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(currentLore);
        }
        return this;
    }
    
    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }
    
    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}