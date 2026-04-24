package com.bloodline.crates.loadout;

import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.CrateKey;

import java.util.ArrayList;
import java.util.List;

public record LoadoutData(
    String pluginVersion,
    long createdAt,
    List<Crate> crates,
    List<CrateKey> keys
) {
    public LoadoutData {
        crates = crates == null ? new ArrayList<>() : new ArrayList<>(crates);
        keys = keys == null ? new ArrayList<>() : new ArrayList<>(keys);
    }
}
