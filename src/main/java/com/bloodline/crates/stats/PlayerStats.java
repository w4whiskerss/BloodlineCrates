package com.bloodline.crates.stats;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStats {
    private UUID playerUUID;
    private String playerName;
    private Map<String, Integer> opensPerCrate;
    private Map<String, String> rarestRewardPerCrate;
    private Map<String, Double> rarestChancePerCrate;
    private Map<String, Integer> currentPityPerCrate;
    private Map<String, Long> lastOpenedPerCrate;
    private int totalOpensAllTime;
    private long firstOpenTimestamp;

    public PlayerStats(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.playerName = "";
        this.opensPerCrate = new LinkedHashMap<>();
        this.rarestRewardPerCrate = new LinkedHashMap<>();
        this.rarestChancePerCrate = new LinkedHashMap<>();
        this.currentPityPerCrate = new LinkedHashMap<>();
        this.lastOpenedPerCrate = new LinkedHashMap<>();
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName == null ? "" : playerName;
    }

    public Map<String, Integer> getOpensPerCrate() {
        return opensPerCrate;
    }

    public void setOpensPerCrate(Map<String, Integer> opensPerCrate) {
        this.opensPerCrate = opensPerCrate == null ? new LinkedHashMap<>() : new LinkedHashMap<>(opensPerCrate);
    }

    public Map<String, String> getRarestRewardPerCrate() {
        return rarestRewardPerCrate;
    }

    public void setRarestRewardPerCrate(Map<String, String> rarestRewardPerCrate) {
        this.rarestRewardPerCrate = rarestRewardPerCrate == null ? new LinkedHashMap<>() : new LinkedHashMap<>(rarestRewardPerCrate);
    }

    public Map<String, Double> getRarestChancePerCrate() {
        return rarestChancePerCrate;
    }

    public void setRarestChancePerCrate(Map<String, Double> rarestChancePerCrate) {
        this.rarestChancePerCrate = rarestChancePerCrate == null ? new LinkedHashMap<>() : new LinkedHashMap<>(rarestChancePerCrate);
    }

    public Map<String, Integer> getCurrentPityPerCrate() {
        return currentPityPerCrate;
    }

    public void setCurrentPityPerCrate(Map<String, Integer> currentPityPerCrate) {
        this.currentPityPerCrate = currentPityPerCrate == null ? new LinkedHashMap<>() : new LinkedHashMap<>(currentPityPerCrate);
    }

    public Map<String, Long> getLastOpenedPerCrate() {
        return lastOpenedPerCrate;
    }

    public void setLastOpenedPerCrate(Map<String, Long> lastOpenedPerCrate) {
        this.lastOpenedPerCrate = lastOpenedPerCrate == null ? new LinkedHashMap<>() : new LinkedHashMap<>(lastOpenedPerCrate);
    }

    public int getTotalOpensAllTime() {
        return totalOpensAllTime;
    }

    public void setTotalOpensAllTime(int totalOpensAllTime) {
        this.totalOpensAllTime = Math.max(0, totalOpensAllTime);
    }

    public long getFirstOpenTimestamp() {
        return firstOpenTimestamp;
    }

    public void setFirstOpenTimestamp(long firstOpenTimestamp) {
        this.firstOpenTimestamp = Math.max(0L, firstOpenTimestamp);
    }
}
