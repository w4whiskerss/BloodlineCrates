package com.bloodlinecrates.model;

public record EffectSettings(
        AnimationType animationType,
        int speedTicks,
        int durationTicks,
        boolean particles,
        boolean sounds,
        boolean synced,
        String skipPermission
) {
}
