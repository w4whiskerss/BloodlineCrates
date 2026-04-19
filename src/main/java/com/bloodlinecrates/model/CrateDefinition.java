package com.bloodlinecrates.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CrateDefinition {
    private final String id;
    private String displayName;
    private CrateType type;
    private String keyId;
    private String itemsAdderModel;
    private boolean previewChances;
    private CooldownScope cooldownScope = CooldownScope.PER_CRATE;
    private long cooldownMillis;
    private EffectSettings effectSettings = new EffectSettings(AnimationType.SPIN, 2, 40, true, true, true, "bloodlinecrates.skipanimation");
    private boolean pityEnabled;
    private int pityThreshold;
    private RewardTier pityGuaranteedTier = RewardTier.RARE;
    private final List<CrateLocation> locations = new ArrayList<>();
    private final Map<String, CrateReward> rewards = new HashMap<>();
    private final Map<Integer, List<String>> bundleCommands = new HashMap<>();

    public CrateDefinition(String id) {
        this.id = id.toLowerCase();
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

    public CrateType type() {
        return type;
    }

    public void setType(CrateType type) {
        this.type = type;
    }

    public String keyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String itemsAdderModel() {
        return itemsAdderModel;
    }

    public void setItemsAdderModel(String itemsAdderModel) {
        this.itemsAdderModel = itemsAdderModel;
    }

    public boolean previewChances() {
        return previewChances;
    }

    public void setPreviewChances(boolean previewChances) {
        this.previewChances = previewChances;
    }

    public CooldownScope cooldownScope() {
        return cooldownScope;
    }

    public void setCooldownScope(CooldownScope cooldownScope) {
        this.cooldownScope = cooldownScope;
    }

    public long cooldownMillis() {
        return cooldownMillis;
    }

    public void setCooldownMillis(long cooldownMillis) {
        this.cooldownMillis = cooldownMillis;
    }

    public EffectSettings effectSettings() {
        return effectSettings;
    }

    public void setEffectSettings(EffectSettings effectSettings) {
        this.effectSettings = effectSettings;
    }

    public boolean pityEnabled() {
        return pityEnabled;
    }

    public void setPityEnabled(boolean pityEnabled) {
        this.pityEnabled = pityEnabled;
    }

    public int pityThreshold() {
        return pityThreshold;
    }

    public void setPityThreshold(int pityThreshold) {
        this.pityThreshold = pityThreshold;
    }

    public RewardTier pityGuaranteedTier() {
        return pityGuaranteedTier;
    }

    public void setPityGuaranteedTier(RewardTier pityGuaranteedTier) {
        this.pityGuaranteedTier = pityGuaranteedTier;
    }

    public List<CrateLocation> locations() {
        return locations;
    }

    public Map<String, CrateReward> rewards() {
        return rewards;
    }

    public Map<Integer, List<String>> bundleCommands() {
        return bundleCommands;
    }
}
