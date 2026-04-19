package com.bloodlinecrates.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CrateReward {
    private final String id;
    private RewardTier tier = RewardTier.COMMON;
    private String permission = "";
    private boolean broadcast;
    private boolean title;
    private boolean bossbar;
    private boolean duplicateProtection;
    private String weightGroup = "default";
    private double chance = 100.0D;
    private ItemStack item;
    private final List<String> commands = new ArrayList<>();

    public CrateReward(String id) {
        this.id = id.toLowerCase();
    }

    public String id() {
        return id;
    }

    public RewardTier tier() {
        return tier;
    }

    public void setTier(RewardTier tier) {
        this.tier = tier;
    }

    public String permission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission == null ? "" : permission;
    }

    public boolean broadcast() {
        return broadcast;
    }

    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    public boolean title() {
        return title;
    }

    public void setTitle(boolean title) {
        this.title = title;
    }

    public boolean bossbar() {
        return bossbar;
    }

    public void setBossbar(boolean bossbar) {
        this.bossbar = bossbar;
    }

    public boolean duplicateProtection() {
        return duplicateProtection;
    }

    public void setDuplicateProtection(boolean duplicateProtection) {
        this.duplicateProtection = duplicateProtection;
    }

    public String weightGroup() {
        return weightGroup;
    }

    public void setWeightGroup(String weightGroup) {
        this.weightGroup = weightGroup;
    }

    public double chance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = chance;
    }

    public ItemStack item() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public List<String> commands() {
        return commands;
    }
}
