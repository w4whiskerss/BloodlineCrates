package com.bloodline.crates.migration.importer;

import com.bloodline.crates.animation.AnimationType;
import com.bloodline.crates.migration.MigrationSource;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.CrateType;
import com.bloodline.crates.model.Reward;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CasinoCratesImporter extends AbstractCrateImporter {
    @Override
    public MigrationSource getSource() {
        return MigrationSource.CASINO_CRATES;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Crate> importCrates(File sourceDirectory) {
        resetMessages();
        if (requireDirectory(sourceDirectory) == null) {
            return List.of();
        }

        List<Crate> crates = new ArrayList<>();
        for (File file : findYamlFiles(sourceDirectory)) {
            YamlConfiguration config = load(file);
            ConfigurationSection cratesSection = config.getConfigurationSection("crates");
            if (cratesSection != null) {
                for (String key : cratesSection.getKeys(false)) {
                    Crate crate = parseCasinoCrate(key, cratesSection.getConfigurationSection(key), file.getName());
                    if (crate != null) {
                        crates.add(crate);
                    }
                }
            } else {
                Crate crate = parseCasinoCrate(baseId(file), config, file.getName());
                if (crate != null) {
                    crates.add(crate);
                }
            }
        }
        return crates;
    }

    private Crate parseCasinoCrate(String fallbackId, ConfigurationSection section, String fileName) {
        if (section == null) {
            return null;
        }

        String id = stringValue(section.getString("id"), fallbackId).toLowerCase();
        String displayName = stringValue(section.getString("display-name"), stringValue(section.getString("name"), id));
        List<Reward> rewards = new ArrayList<>();

        List<Map<String, Object>> rawRewards = new ArrayList<>();
        ConfigurationSection rewardSection = section.getConfigurationSection("rewards");
        if (rewardSection != null) {
            rawRewards.addAll(sectionChildren(rewardSection));
        } else if (section.getList("rewards") instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> map) {
                    rawRewards.add((Map<String, Object>) map);
                }
            }
        }

        int index = 0;
        for (Map<String, Object> rewardMap : rawRewards) {
            double chance = doubleValue(rewardMap.get("chance"), doubleValue(rewardMap.get("weight"), 0.0D));
            Object commandObj = rewardMap.get("command");
            if (commandObj == null) {
                commandObj = rewardMap.get("commands");
            }
            if (commandObj instanceof String command) {
                rewards.add(createCommandReward(id + "_reward_" + index, command, chance, "&eMigrated Command"));
            } else if (rewardMap.get("item") instanceof Map<?, ?> map) {
                ItemStack item = parseItemMap(map, fileName + " reward " + index);
                if (item != null) {
                    rewards.add(createItemReward(
                        id + "_reward_" + index,
                        item,
                        chance,
                        item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : "&eMigrated Item"
                    ));
                }
            } else {
                warnings.add(fileName + ": skipped reward " + index + " because it had no supported type");
            }
            index++;
        }

        if (rewards.isEmpty()) {
            warnings.add(fileName + ": no importable rewards found for " + id);
            return null;
        }

        return createCrate(id, displayName, CrateType.RANDOM, AnimationType.ROULETTE, rewards, defaultKeyItem(displayName));
    }
}
