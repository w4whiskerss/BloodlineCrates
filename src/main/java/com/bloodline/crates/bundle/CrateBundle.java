package com.bloodline.crates.bundle;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CrateBundle {
    private String id;
    private String displayName;
    private List<BundleEntry> contents;

    @Data
    @AllArgsConstructor
    public static class BundleEntry {
        private String crateId;
        private int amount;
    }
}
