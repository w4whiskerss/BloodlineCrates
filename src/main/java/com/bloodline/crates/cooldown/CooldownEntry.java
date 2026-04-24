package com.bloodline.crates.cooldown;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class CooldownEntry {
    private UUID playerUUID;
    private String crateId;
    private long lastOpenedAt;
    private long cooldownMillis;
}
