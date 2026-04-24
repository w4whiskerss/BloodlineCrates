package com.bloodline.crates.animation;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnimationConfig {
    private AnimationType type;
    private int durationTicks;
    private String particleType;
    private int particleCount;
    private String openSound;
    private String revealSound;
    private String skipPermission;
}