package com.bloodline.crates.migration.importer;

import com.bloodline.crates.animation.AnimationConfig;
import com.bloodline.crates.animation.AnimationType;
import com.bloodline.crates.migration.MigrationSource;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.CrateType;
import com.bloodline.crates.model.Reward;
import com.bloodline.crates.reward.RewardType;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

abstract class AbstractCrateImporter implements CrateImporter {
    protected final List<String> warnings = new ArrayList<>();
    protected final List<String> errors = new ArrayList<>();

    @Override
    public List<String> getWarnings() {
        return warnings;
    }

    @Override
    public List<String> getErrors() {
        return errors;
    }

    protected void resetMessages() {
        warnings.clear();
        errors.clear();
    }

    protected File requireDirectory(File sourceDirectory) {
        if (sourceDirectory == null || !sourceDirectory.exists() || !sourceDirectory.isDirectory()) {
            errors.add("Source data folder does not exist: " + (sourceDirectory == null ? "null" : sourceDirectory.getAbsolutePath()));
            return null;
        }
        return sourceDirectory;
    }

    protected List<File> findYamlFiles(File sourceDirectory) {
        List<File> files = new ArrayList<>();
        collectYamlFiles(sourceDirectory, files);
        return files;
    }

    private void collectYamlFiles(File directory, List<File> files) {
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectYamlFiles(child, files);
            } else if (child.getName().toLowerCase(Locale.ROOT).endsWith(".yml")
                || child.getName().toLowerCase(Locale.ROOT).endsWith(".yaml")) {
                files.add(child);
            }
        }
    }

    protected AnimationConfig defaultAnimation(AnimationType type) {
        return new AnimationConfig(type, 60, "FLAME", 30, "BLOCK_CHEST_OPEN", "ENTITY_PLAYER_LEVELUP", "bloodcrates.skip");
    }

    protected ItemStack defaultKeyItem(String crateName) {
        ItemStack item = XMaterial.TRIPWIRE_HOOK.parseItem();
        if (item == null) {
            item = new ItemStack(Material.TRIPWIRE_HOOK);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&6" + crateName + " Key"));
            meta.setLore(List.of(color("&7Generated during migration")));
            item.setItemMeta(meta);
        }
        return item;
    }

    protected Crate createCrate(String id, String displayName, CrateType crateType, AnimationType animationType, List<Reward> rewards, ItemStack keyItem) {
        return new Crate(
            id,
            color(displayName),
            crateType,
            id + "_key",
            rewards,
            defaultAnimation(animationType),
            false,
            0,
            null,
            "",
            keyItem,
            6,
            Collections.emptyList()
        );
    }

    protected Reward createCommandReward(String id, String command, double chance, String displayName) {
        Reward reward = new Reward(id, RewardType.COMMAND, null, chance, "", false, 0.0D, sanitizeCommand(command), displayName, null);
        reward.setDisplayItem(defaultDisplayItem(Material.COMMAND_BLOCK, displayName, "&7Migrated command reward"));
        return reward;
    }

    protected Reward createItemReward(String id, ItemStack item, double chance, String displayName) {
        return new Reward(id, RewardType.ITEM, item, chance, "", false, 0.0D, null, displayName, null);
    }

    protected Reward createMultiReward(String id, List<Reward> subRewards, double chance, String displayName) {
        Reward reward = new Reward(id, RewardType.MULTI, null, chance, "", false, 0.0D, null, displayName, null);
        reward.setDisplayItem(defaultDisplayItem(Material.CHEST, displayName, "&7Migrated multi reward"));
        reward.setSubRewards(subRewards);
        return reward;
    }

    protected ItemStack defaultDisplayItem(Material material, String displayName, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null && !displayName.isBlank()) {
                meta.setDisplayName(color(displayName));
            }
            meta.setLore(List.of(color(loreLine)));
            item.setItemMeta(meta);
        }
        return item;
    }

    protected String sanitizeCommand(String command) {
        if (command == null) {
            return null;
        }
        return command.trim().replaceFirst("^/+", "");
    }

    protected String color(String input) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    protected Optional<XMaterial> matchMaterial(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return XMaterial.matchXMaterial(raw.toUpperCase(Locale.ROOT));
    }

    protected ItemStack parseItemMap(Map<?, ?> rawMap, String context) {
        Object materialObj = rawMap.get("material");
        if (materialObj == null) {
            materialObj = rawMap.get("type");
        }
        Optional<XMaterial> material = matchMaterial(materialObj == null ? null : String.valueOf(materialObj));
        if (material.isEmpty()) {
            warnings.add(context + ": unknown item material " + materialObj);
            return null;
        }

        ItemStack item = material.get().parseItem();
        if (item == null) {
            warnings.add(context + ": failed to parse item material " + materialObj);
            return null;
        }

        int amount = intValue(rawMap.get("amount"), 1);
        item.setAmount(Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = stringValue(rawMap.get("name"), stringValue(rawMap.get("display-name"), null));
            if (name != null && !name.isBlank()) {
                meta.setDisplayName(color(name));
            }
            Object loreObj = rawMap.get("lore");
            if (loreObj instanceof List<?> loreList) {
                List<String> lore = new ArrayList<>();
                for (Object line : loreList) {
                    lore.add(color(String.valueOf(line)));
                }
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    protected List<Map<String, Object>> sectionChildren(ConfigurationSection section) {
        List<Map<String, Object>> children = new ArrayList<>();
        if (section == null) {
            return children;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection child = section.getConfigurationSection(key);
            if (child != null) {
                Map<String, Object> values = new LinkedHashMap<>(child.getValues(false));
                values.putIfAbsent("id", key);
                children.add(values);
            }
        }
        return children;
    }

    protected String baseId(File file) {
        String name = file.getName();
        int index = name.lastIndexOf('.');
        return (index >= 0 ? name.substring(0, index) : name).toLowerCase(Locale.ROOT);
    }

    protected String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    protected double doubleValue(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    protected int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    protected YamlConfiguration load(File file) {
        return YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public boolean isPluginPresent() {
        String pluginName = switch (getSource()) {
            case CRATE_RELOADED -> "CrateReloaded";
            case EPIC_CRATES -> "EpicCrates";
            case CASINO_CRATES -> "CasinoCrates";
        };
        return Bukkit.getPluginManager().getPlugin(pluginName) != null;
    }
}
