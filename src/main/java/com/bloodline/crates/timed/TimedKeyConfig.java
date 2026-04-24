package com.bloodline.crates.timed;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TimedKeyConfig {
    private final String crateId;
    private final int amount;
    private final TimedKeyInterval interval;
    private final long intervalTicks;
}