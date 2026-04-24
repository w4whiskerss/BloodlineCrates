package com.bloodline.crates.model;

import com.bloodline.crates.reward.RewardType;
import lombok.Data;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
public class Reward {
    private String id;
    private RewardType rewardType = RewardType.ITEM;
    private ItemStack item;
    private ItemStack displayItem;
    private double chance;
    private String requiredPermission;
    private String command;
    private double economyAmount;
    private boolean broadcast;
    private String broadcastMessage;
    private String crateId;
    private List<Reward> subRewards = new ArrayList<>();
    private boolean hidden;
    private String displayName;
    private String description;

    public Reward(ItemStack item, double chance, String requiredPermission) {
        this(null, RewardType.ITEM, item, chance, requiredPermission, false, 0.0D, null, null, null);
    }

    public Reward(String id, ItemStack item, double chance, String requiredPermission) {
        this(id, RewardType.ITEM, item, chance, requiredPermission, false, 0.0D, null, null, null);
    }

    public Reward(
        String id,
        ItemStack item,
        double chance,
        String requiredPermission,
        boolean economyRewardEnabled,
        double economyRewardAmount
    ) {
        this(id, economyRewardEnabled ? RewardType.ECONOMY : RewardType.ITEM, item, chance, requiredPermission, economyRewardEnabled, economyRewardAmount, null, null, null);
    }

    public Reward(
        String id,
        RewardType rewardType,
        ItemStack item,
        double chance,
        String requiredPermission,
        boolean economyRewardEnabled,
        double economyRewardAmount,
        String command,
        String displayName,
        String description
    ) {
        this.id = id;
        this.rewardType = rewardType == null ? RewardType.ITEM : rewardType;
        this.item = item;
        this.displayItem = item == null ? null : item.clone();
        this.chance = chance;
        this.requiredPermission = requiredPermission;
        this.command = command;
        this.economyAmount = economyRewardEnabled ? economyRewardAmount : (this.rewardType == RewardType.ECONOMY ? economyRewardAmount : 0.0D);
        this.displayName = displayName;
        this.description = description;
        applyTextToItems();
    }

    public boolean hasPermission() {
        return requiredPermission != null && !requiredPermission.isEmpty();
    }

    public boolean isCommandReward() {
        return rewardType == RewardType.COMMAND;
    }

    public boolean isEconomyRewardEnabled() {
        return rewardType == RewardType.ECONOMY || economyAmount > 0.0D;
    }

    public double getEconomyRewardAmount() {
        return economyAmount;
    }

    public void setEconomyRewardAmount(double amount) {
        this.economyAmount = amount;
        if (amount > 0.0D && rewardType == RewardType.ITEM) {
            this.rewardType = RewardType.ECONOMY;
        }
    }

    public boolean isEconomyRewardLegacyEnabled() {
        return economyAmount > 0.0D;
    }

    public double getEconomyAmount() {
        return economyAmount;
    }

    public ItemStack getDisplayItem() {
        if (hidden) {
            ItemStack hiddenItem = new ItemStack(Material.BARRIER);
            ItemMeta meta = hiddenItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "???");
                hiddenItem.setItemMeta(meta);
            }
            return hiddenItem;
        }

        ItemStack source = rewardType == RewardType.ITEM ? item : (displayItem != null ? displayItem : item);
        return source == null ? null : source.clone();
    }

    public ItemStack getStoredDisplayItem() {
        return displayItem == null ? null : displayItem.clone();
    }

    public String getDisplayNameOrFallback() {
        if (hidden) {
            return ChatColor.RED + "???";
        }
        if (displayName != null && !displayName.isBlank()) {
            return ChatColor.translateAlternateColorCodes('&', displayName);
        }
        ItemStack shown = getDisplayItem();
        if (shown != null && shown.hasItemMeta() && shown.getItemMeta().hasDisplayName()) {
            return shown.getItemMeta().getDisplayName();
        }
        if (crateId != null && !crateId.isBlank()) {
            return crateId;
        }
        if (shown != null) {
            return shown.getType().name();
        }
        return "Reward";
    }

    public List<String> getDescriptionLines() {
        if (description == null || description.isBlank()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(ChatColor.translateAlternateColorCodes('&', description));
    }

    public Reward copy() {
        Reward copy = new Reward(
            id,
            rewardType,
            item == null ? null : item.clone(),
            chance,
            requiredPermission,
            rewardType == RewardType.ECONOMY,
            economyAmount,
            command,
            displayName,
            description
        );
        copy.setDisplayItem(displayItem == null ? null : displayItem.clone());
        copy.setBroadcast(broadcast);
        copy.setBroadcastMessage(broadcastMessage);
        copy.setCrateId(crateId);
        copy.setSubRewards(subRewards == null ? new ArrayList<>() : subRewards.stream().map(Reward::copy).toList());
        copy.setHidden(hidden);
        return copy;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        applyTextToItems();
    }

    public void setDescription(String description) {
        this.description = description;
        applyTextToItems();
    }

    public void setItem(ItemStack item) {
        this.item = item;
        if (rewardType == RewardType.ITEM || displayItem == null) {
            this.displayItem = item == null ? null : item.clone();
        }
        applyTextToItems();
    }

    public void setDisplayItem(ItemStack displayItem) {
        this.displayItem = displayItem;
        applyTextToItems();
    }

    private void applyTextToItems() {
        applyText(item);
        if (displayItem != null && displayItem != item) {
            applyText(displayItem);
        }
    }

    private void applyText(ItemStack stack) {
        if (stack == null) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.setDisplayName(displayName == null || displayName.isBlank()
            ? meta.hasDisplayName() ? meta.getDisplayName() : null
            : ChatColor.translateAlternateColorCodes('&', displayName));
        if (description != null && !description.isBlank()) {
            meta.setLore(getDescriptionLines());
        }
        stack.setItemMeta(meta);
    }
}
