package com.bloodline.crates.migration;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.migration.importer.CasinoCratesImporter;
import com.bloodline.crates.migration.importer.CrateImporter;
import com.bloodline.crates.migration.importer.CrateReloadedImporter;
import com.bloodline.crates.migration.importer.EpicCratesImporter;
import com.bloodline.crates.model.Crate;

import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MigrationManager {
    private final BloodlineCrates plugin;
    private final Map<MigrationSource, CrateImporter> importers = new EnumMap<>(MigrationSource.class);

    public MigrationManager(BloodlineCrates plugin) {
        this.plugin = plugin;
        register(new CrateReloadedImporter());
        register(new EpicCratesImporter());
        register(new CasinoCratesImporter());
    }

    public MigrationResult dryRun(MigrationSource source) {
        return runImport(source);
    }

    public MigrationResult confirm(MigrationSource source, boolean overwrite) {
        MigrationResult result = runImport(source);
        int written = 0;
        int skipped = result.getCratesSkipped();

        for (Crate crate : result.getImportedCrates()) {
            boolean exists = plugin.getConfigManager().getCrate(crate.getId()).isPresent();
            if (exists && !overwrite) {
                skipped++;
                result.getWarnings().add("Skipped existing crate " + crate.getId() + " (use --overwrite to replace)");
                continue;
            }
            plugin.getConfigManager().saveCrate(crate);
            written++;
        }

        result.setCratesImported(written);
        result.setCratesSkipped(skipped);
        try {
            plugin.getConfigManager().loadAll();
        } catch (Exception exception) {
            result.getErrors().add("Failed to reload imported crates: " + exception.getMessage());
        }
        return result;
    }

    private void register(CrateImporter importer) {
        importers.put(importer.getSource(), importer);
    }

    private MigrationResult runImport(MigrationSource source) {
        MigrationResult result = new MigrationResult();
        CrateImporter importer = importers.get(source);
        if (importer == null) {
            result.getErrors().add("No importer registered for " + source);
            return result;
        }

        File sourceDirectory = resolveSourceDirectory(source);
        List<Crate> crates = importer.importCrates(sourceDirectory);
        result.setImportedCrates(crates);
        result.setCratesImported(crates.size());
        result.getWarnings().addAll(importer.getWarnings());
        result.getErrors().addAll(importer.getErrors());
        result.setRewardsImported(crates.stream().mapToInt(crate -> crate.getRewards().size()).sum());
        return result;
    }

    private File resolveSourceDirectory(MigrationSource source) {
        File pluginsFolder = plugin.getDataFolder().getParentFile();
        String name = switch (source) {
            case CRATE_RELOADED -> "CrateReloaded";
            case EPIC_CRATES -> "EpicCrates";
            case CASINO_CRATES -> "CasinoCrates";
        };
        return new File(pluginsFolder, name);
    }
}
