package com.bloodline.crates.audit;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class AuditEntry {
    private UUID actorUUID;
    private String actorName;
    private AuditEventType eventType;
    private String targetId;
    private String detail;
    private long timestamp;

    public enum AuditEventType {
        CRATE_OPEN,
        KEY_GIVEN,
        REWARD_GRANTED,
        CONFIG_CHANGED,
        EDITOR_ACTION
    }
}
