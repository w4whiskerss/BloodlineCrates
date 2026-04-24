package com.bloodline.crates.util;

import com.bloodline.crates.model.Reward;
import com.bloodline.crates.reward.RewardType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RewardStorageUtil {
    private RewardStorageUtil() {
    }

    public static void writeReward(ConfigurationSection section, Reward reward) {
        section.set("id", reward.getId());
        section.set("type", reward.getRewardType().name());
        section.set("reward-type", reward.getRewardType().name());
        section.set("itemstack", reward.getItem());
        section.set("display-item", reward.getRewardType() == RewardType.ITEM ? null : reward.getStoredDisplayItem());
        section.set("chance", reward.getChance());
        section.set("permission", reward.getRequiredPermission() == null ? "" : reward.getRequiredPermission());
        section.set("command", reward.getCommand());
        section.set("economy-amount", reward.getEconomyAmount());
        section.set("broadcast", reward.isBroadcast());
        section.set("broadcast-message", reward.getBroadcastMessage());
        section.set("crate-id", reward.getCrateId());
        section.set("hidden", reward.isHidden());
        section.set("display-name", reward.getDisplayName());
        section.set("description", reward.getDescription());

        if (reward.getSubRewards() != null && !reward.getSubRewards().isEmpty()) {
            List<Object> serialized = new ArrayList<>();
            for (Reward subReward : reward.getSubRewards()) {
                ConfigurationSection child = section.createSection("sub-rewards-temp." + serialized.size());
                writeReward(child, subReward);
                serialized.add(child.getValues(true));
            }
            section.set("sub-rewards-temp", null);
            section.set("sub-rewards", serialized);
        } else {
            section.set("sub-rewards", null);
        }
    }

    @SuppressWarnings("unchecked")
    public static Reward readReward(ConfigurationSection section) {
        RewardType rewardType = resolveRewardType(section);

        ItemStack item = section.getItemStack("itemstack");
        if (item == null && section.contains("item")) {
            Object old = section.get("item");
            if (old instanceof ItemStack stack) {
                item = stack;
            }
        }

        ItemStack displayItem = section.getItemStack("display-item");
        if (displayItem == null && section.contains("displayItem")) {
            Object raw = section.get("displayItem");
            if (raw instanceof ItemStack stack) {
                displayItem = stack;
            }
        }

        if (rewardType == RewardType.ITEM && item == null) {
            return null;
        }

        Reward reward = new Reward(
            section.getString("id"),
            rewardType,
            item,
            section.getDouble("chance", 0.0D),
            section.getString("permission", ""),
            rewardType == RewardType.ECONOMY || section.getBoolean("economy-reward.enabled", false),
            firstPositive(section.getDouble("economy-amount", 0.0D), section.getDouble("economy-reward.amount", 0.0D)),
            section.getString("command"),
            section.getString("display-name"),
            section.getString("description")
        );
        reward.setDisplayItem(displayItem == null ? (item == null ? null : item.clone()) : displayItem);
        reward.setBroadcast(section.getBoolean("broadcast", false));
        reward.setBroadcastMessage(section.getString("broadcast-message"));
        reward.setCrateId(section.getString("crate-id"));
        reward.setHidden(section.getBoolean("hidden", false));

        List<Reward> subRewards = new ArrayList<>();
        List<?> rawSubRewards = section.getList("sub-rewards");
        if (rawSubRewards != null) {
            for (Object entry : rawSubRewards) {
                if (entry instanceof java.util.Map<?, ?> map) {
                    ConfigurationSection nested = mapToSection(map);
                    Reward subReward = readReward(nested);
                    if (subReward != null) {
                        subRewards.add(subReward);
                    }
                }
            }
        }
        reward.setSubRewards(subRewards);
        return reward;
    }

    private static RewardType resolveRewardType(ConfigurationSection section) {
        String rawType = section.getString("type", section.getString("reward-type", "ITEM"));
        try {
            return RewardType.valueOf(rawType.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return RewardType.ITEM;
        }
    }

    private static double firstPositive(double primary, double fallback) {
        return primary > 0.0D ? primary : fallback;
    }

    private static ConfigurationSection mapToSection(Map<?, ?> source) {
        MemoryConfiguration configuration = new MemoryConfiguration();
        ConfigurationSection section = configuration.createSection("reward");
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nestedMap) {
                section.createSection(key, new LinkedHashMap<>(nestedMap));
            } else {
                section.set(key, value);
            }
        }
        return section;
    }
}
