package com.bloodlinecrates.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CrateKey {
    private final String id;
    private String displayName;
    private List<String> lore;
    private String itemsAdderModel;
    private boolean physicalEnabled;
    private boolean virtualEnabled;
    private ItemStack item;
    private final List<TimedKeyRule> timedKeyRules;

    public CrateKey(String id) {
        this.id = id.toLowerCase();
        this.lore = new ArrayList<>();
        this.timedKeyRules = new ArrayList<>();
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> lore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    public String itemsAdderModel() {
        return itemsAdderModel;
    }

    public void setItemsAdderModel(String itemsAdderModel) {
        this.itemsAdderModel = itemsAdderModel;
    }

    public boolean physicalEnabled() {
        return physicalEnabled;
    }

    public void setPhysicalEnabled(boolean physicalEnabled) {
        this.physicalEnabled = physicalEnabled;
    }

    public boolean virtualEnabled() {
        return virtualEnabled;
    }

    public void setVirtualEnabled(boolean virtualEnabled) {
        this.virtualEnabled = virtualEnabled;
    }

    public ItemStack item() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public List<TimedKeyRule> timedKeyRules() {
        return timedKeyRules;
    }
}
