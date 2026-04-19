package com.bloodlinecrates.model;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerProfile {
    private final UUID uniqueId;
    private final Map<String, Integer> virtualKeys = new HashMap<>();
    private final Map<String, Integer> pityCounters = new HashMap<>();
    private final Map<String, Long> crateCooldowns = new HashMap<>();
    private final Map<String, Long> timedKeyNextClaim = new HashMap<>();
    private final Map<LeaderboardPeriod, Integer> crateOpensByPeriod = new EnumMap<>(LeaderboardPeriod.class);
    private long globalCooldownUntil;
    private long totalCratesOpened;
    private String lastReward = "";
    private String lastKnownAddress = "";
    private Instant lastOpen = Instant.EPOCH;

    public PlayerProfile(UUID uniqueId) {
        this.uniqueId = uniqueId;
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            crateOpensByPeriod.put(period, 0);
        }
    }

    public UUID uniqueId() {
        return uniqueId;
    }

    public Map<String, Integer> virtualKeys() {
        return virtualKeys;
    }

    public Map<String, Integer> pityCounters() {
        return pityCounters;
    }

    public Map<String, Long> crateCooldowns() {
        return crateCooldowns;
    }

    public Map<String, Long> timedKeyNextClaim() {
        return timedKeyNextClaim;
    }

    public Map<LeaderboardPeriod, Integer> crateOpensByPeriod() {
        return crateOpensByPeriod;
    }

    public long globalCooldownUntil() {
        return globalCooldownUntil;
    }

    public void setGlobalCooldownUntil(long globalCooldownUntil) {
        this.globalCooldownUntil = globalCooldownUntil;
    }

    public long totalCratesOpened() {
        return totalCratesOpened;
    }

    public void incrementTotalCratesOpened() {
        this.totalCratesOpened++;
    }

    public String lastReward() {
        return lastReward;
    }

    public void setLastReward(String lastReward) {
        this.lastReward = lastReward;
    }

    public String lastKnownAddress() {
        return lastKnownAddress;
    }

    public void setLastKnownAddress(String lastKnownAddress) {
        this.lastKnownAddress = lastKnownAddress;
    }

    public Instant lastOpen() {
        return lastOpen;
    }

    public void setLastOpen(Instant lastOpen) {
        this.lastOpen = lastOpen;
    }
}
