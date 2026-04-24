package com.bloodline.crates.debug;

import java.util.UUID;

public record DebugEvent(DebugEventType type, UUID subjectPlayerUUID, String detail, long timestamp) {
}
