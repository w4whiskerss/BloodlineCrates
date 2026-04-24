package com.bloodline.crates.migration;

import com.bloodline.crates.model.Crate;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MigrationResult {
    private int cratesImported;
    private int cratesSkipped;
    private int rewardsImported;
    private int rewardsSkipped;
    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private List<Crate> importedCrates = new ArrayList<>();
}
