package com.bloodline.crates.migration.importer;

import com.bloodline.crates.migration.MigrationSource;
import com.bloodline.crates.model.Crate;

import java.io.File;
import java.util.List;

public interface CrateImporter {
    MigrationSource getSource();

    boolean isPluginPresent();

    List<Crate> importCrates(File sourceDirectory);

    List<String> getWarnings();

    List<String> getErrors();
}
