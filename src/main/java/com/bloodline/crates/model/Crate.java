package com.bloodline.crates.model;

import com.bloodline.crates.animation.AnimationConfig;
import lombok.Data;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
public class Crate {
    private String id;
    private String displayName;
    private CrateType type;
    private String keyId;
    private List<Reward> rewards;
    private AnimationConfig animationConfig;
    private boolean pityEnabled;
    private int pityThreshold;
    private String jackpotRewardId;
    private String description;
    private ItemStack keyDisplayItem;
    private int guiRows;
    private List<Integer> rewardSlots;
    private boolean hologramEnabled = true;
    private List<String> hologramLines = new ArrayList<>();
    private boolean cooldownEnabled;
    private long cooldownDurationSeconds;
    private String cooldownBypassPermission = "bloodcrates.cooldown.bypass";
    private String cooldownMessage = "&cYou must wait &e{time} &cbefore opening this crate again.";
    private KeyMode keyMode = KeyMode.PHYSICAL;
    private boolean virtualOnly;
    private boolean globalLimitEnabled;
    private int globalLimitMax;
    private String globalLimitMessage = "&cThis crate has reached its global open limit!";
    private String globalLimitOnReached = "DISABLE";
    private boolean perPlayerLimitEnabled;
    private int perPlayerLimitMax;
    private String perPlayerLimitMessage = "&cYou have reached the maximum opens for this crate.";
    private String perPlayerResetInterval = "NEVER";

    public Crate(String id, String displayName, CrateType type, String keyId, List<Reward> rewards, AnimationConfig animationConfig) {
        this(id, displayName, type, keyId, rewards, animationConfig, false, 0, null, "", null, 6, Collections.emptyList());
    }

    public Crate(
        String id,
        String displayName,
        CrateType type,
        String keyId,
        List<Reward> rewards,
        AnimationConfig animationConfig,
        boolean pityEnabled,
        int pityThreshold,
        String jackpotRewardId
    ) {
        this(id, displayName, type, keyId, rewards, animationConfig, pityEnabled, pityThreshold, jackpotRewardId, "", null, 6, Collections.emptyList());
    }

    public Crate(
        String id,
        String displayName,
        CrateType type,
        String keyId,
        List<Reward> rewards,
        AnimationConfig animationConfig,
        boolean pityEnabled,
        int pityThreshold,
        String jackpotRewardId,
        String description,
        ItemStack keyDisplayItem
    ) {
        this(id, displayName, type, keyId, rewards, animationConfig, pityEnabled, pityThreshold, jackpotRewardId, description, keyDisplayItem, 6, Collections.emptyList());
    }

    public Crate(
        String id,
        String displayName,
        CrateType type,
        String keyId,
        List<Reward> rewards,
        AnimationConfig animationConfig,
        boolean pityEnabled,
        int pityThreshold,
        String jackpotRewardId,
        String description,
        ItemStack keyDisplayItem,
        int guiRows,
        List<Integer> rewardSlots
    ) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.keyId = keyId;
        this.rewards = rewards;
        this.animationConfig = animationConfig;
        this.pityEnabled = pityEnabled;
        this.pityThreshold = pityThreshold;
        this.jackpotRewardId = jackpotRewardId;
        this.description = description;
        this.keyDisplayItem = keyDisplayItem;
        this.guiRows = guiRows;
        this.rewardSlots = rewardSlots == null ? new ArrayList<>() : new ArrayList<>(rewardSlots);
    }

    public List<String> getDescriptionLines() {
        if (description == null || description.isBlank()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(ChatColor.translateAlternateColorCodes('&', description));
    }

    public int getGuiRowsClamped() {
        return Math.max(1, Math.min(6, guiRows));
    }

    public int getCloseButtonSlot() {
        return getGuiRowsClamped() * 9 - 1;
    }

    public List<Integer> getEffectiveRewardSlots() {
        List<Integer> configured = rewardSlots == null ? Collections.emptyList() : rewardSlots;
        if (!configured.isEmpty()) {
            return configured.stream()
                .filter(slot -> slot >= 0 && slot < getCloseButtonSlot())
                .distinct()
                .sorted()
                .toList();
        }

        List<Integer> defaults = new ArrayList<>();
        for (int slot = 0; slot < getCloseButtonSlot(); slot++) {
            defaults.add(slot);
        }
        return defaults;
    }
}
