package com.bloodline.crates.loadout;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.loadout.impl.JsonLoadoutSerializer;
import com.bloodline.crates.loadout.impl.YamlLoadoutSerializer;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.CrateKey;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LoadoutManager {
    public enum LoadMode {
        MERGE,
        OVERWRITE
    }

    private final BloodlineCrates plugin;

    public LoadoutManager(BloodlineCrates plugin) {
        this.plugin = plugin;
        getLoadoutsFolder().mkdirs();
        getBackupsFolder().mkdirs();
    }

    public File saveLoadout(String name) throws IOException {
        backupCurrentCrates("pre-load");

        LoadoutData data = new LoadoutData(
            plugin.getPluginMeta().getVersion(),
            System.currentTimeMillis(),
            new ArrayList<>(plugin.getConfigManager().getAllCrates()),
            new ArrayList<>(plugin.getKeyManager().getAllKeys())
        );

        File file = new File(getLoadoutsFolder(), sanitizeName(name) + serializer().getFileExtension());
        serializer().write(file, data);
        return file;
    }

    public void loadLoadout(String name, LoadMode mode) throws IOException, LoadoutValidationException, org.bukkit.configuration.InvalidConfigurationException {
        File file = resolveLoadoutFile(name);
        LoadoutData data = serializerFor(file).read(file);
        LoadoutValidator.validate(data);

        backupCurrentCrates("pre-load");
        if (mode == LoadMode.OVERWRITE) {
            deleteAllCrateFiles();
        }

        for (Crate crate : data.crates()) {
            plugin.getConfigManager().saveCrate(crate);
        }
        for (CrateKey crateKey : data.keys()) {
            plugin.getKeyManager().saveKey(crateKey);
        }
        plugin.getConfigManager().reload();
    }

    public void rollbackLatest() throws IOException, org.bukkit.configuration.InvalidConfigurationException {
        File latestBackup = getLatestBackupFolder();
        if (latestBackup == null) {
            throw new IOException("No rollback backup found.");
        }

        deleteAllCrateFiles();
        copyDirectory(latestBackup.toPath(), getCratesFolder().toPath());
        plugin.getConfigManager().reload();
    }

    public boolean deleteLoadout(String name) {
        File file = findLoadoutFile(name);
        return file != null && file.exists() && file.delete();
    }

    public List<String> listLoadouts() {
        File[] files = getLoadoutsFolder().listFiles((dir, fileName) -> fileName.endsWith(".json") || fileName.endsWith(".yml"));
        if (files == null) {
            return List.of();
        }
        return java.util.Arrays.stream(files)
            .map(File::getName)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    private void backupCurrentCrates(String prefix) throws IOException {
        File cratesFolder = getCratesFolder();
        if (!cratesFolder.exists()) {
            cratesFolder.mkdirs();
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        File backupFolder = new File(getBackupsFolder(), prefix + "-" + timestamp);
        backupFolder.mkdirs();
        copyDirectory(cratesFolder.toPath(), backupFolder.toPath());
    }

    private void deleteAllCrateFiles() throws IOException {
        File[] files = getCratesFolder().listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            Files.deleteIfExists(file.toPath());
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            return;
        }
        Files.createDirectories(target);
        try (var stream = Files.walk(source)) {
            for (Path path : stream.toList()) {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private File resolveLoadoutFile(String name) throws IOException {
        File file = findLoadoutFile(name);
        if (file == null) {
            throw new IOException("Loadout not found: " + name);
        }
        return file;
    }

    private File findLoadoutFile(String name) {
        String sanitized = sanitizeName(name);
        File json = new File(getLoadoutsFolder(), sanitized + ".json");
        if (json.exists()) {
            return json;
        }
        File yaml = new File(getLoadoutsFolder(), sanitized + ".yml");
        if (yaml.exists()) {
            return yaml;
        }
        return null;
    }

    private File getLatestBackupFolder() {
        File[] backups = getBackupsFolder().listFiles(File::isDirectory);
        if (backups == null || backups.length == 0) {
            return null;
        }
        return java.util.Arrays.stream(backups)
            .max(Comparator.comparingLong(File::lastModified))
            .orElse(null);
    }

    private LoadoutSerializer serializer() {
        String format = plugin.getConfig().getString("loadout.format", "JSON").toUpperCase(Locale.ROOT);
        return "YAML".equals(format) || "YML".equals(format) ? new YamlLoadoutSerializer() : new JsonLoadoutSerializer();
    }

    private LoadoutSerializer serializerFor(File file) {
        return file.getName().endsWith(".yml") ? new YamlLoadoutSerializer() : new JsonLoadoutSerializer();
    }

    private String sanitizeName(String name) {
        return name == null ? "" : name.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private File getLoadoutsFolder() {
        return new File(plugin.getDataFolder(), "loadouts");
    }

    private File getBackupsFolder() {
        return new File(getLoadoutsFolder(), "backups");
    }

    private File getCratesFolder() {
        return new File(plugin.getDataFolder(), "crates");
    }
}
