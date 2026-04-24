package com.bloodline.crates.pity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PityTracker {
    private String crateId;
    private int count;
}
