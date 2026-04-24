package com.bloodline.crates.leaderboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class LeaderboardEntry {
    private UUID playerUUID;
    private String playerName;
    private int crateOpens;
    private long timeAtTopMillis;
}