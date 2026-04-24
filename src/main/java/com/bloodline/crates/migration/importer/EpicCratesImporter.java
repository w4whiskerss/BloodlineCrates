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

public class EpicCratesImporter extends AbstractCrateImporter {
    @Override
    public MigrationSource getSource() {
        return MigrationSource.EPIC_CRATES;
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
            String id = stringValue(config.getString("id"), baseId(file)).toLowerCase();
            String displayName = stringValue(config.getString("name"), id);
            String rawType = stringValue(config.getString("type"), "SPINNER");
            AnimationType animationType = "INSTANT".equalsIgnoreCase(rawType) ? AnimationType.INSTANT : AnimationType.ROULETTE;

            List<Map<String, Object>> rawRewards = new ArrayList<>();
            ConfigurationSection rewardsSection = config.getConfigurationSection("rewards");
            if (rewardsSection != null) {
                rawRewards.addAll(sectionChildren(rewardsSection));
            } else if (config.getList("rewards") instanceof List<?> list) {
                for (Object entry : list) {
                    if (entry instanceof Map<?, ?> map) {
                        rawRewards.add((Map<String, Object>) map);
                    }
                }
            }

            if (rawRewards.isEmpty()) {
                warnings.add(file.getName() + ": no rewards found");
                continue;
            }

            double totalChance = 0.0D;
            for (Map<String, Object> rewardMap : rawRewards) {
                totalChance += doubleValue(rewardMap.get("chance"), 0.0D);
            }
            boolean normalize = totalChance > 0.0D && Math.abs(totalChance - 100.0D) > 0.01D;
            if (normalize) {
                warnings.add(file.getName() + ": normalized reward chances from total " + totalChance + " to 100.0");
            }

            List<Reward> rewards = new ArrayList<>();
            int index = 0;
            for (Map<String, Object> rewardMap : rawRewards) {
                double chance = doubleValue(rewardMap.get("chance"), 0.0D);
                if (normalize) {
                    chance = (chance / totalChance) * 100.0D;
                }

                List<Reward> subRewards = new ArrayList<>();
                Object commands = rewardMap.get("commands");
                if (commands instanceof List<?> commandList) {
                    int cmdIndex = 0;
                    for (Object commandObj : commandList) {
                        String command = sanitizeCommand(String.valueOf(commandObj));
                        if (command != null && !command.isBlank()) {
                            subRewards.add(createCommandReward(id + "_reward_" + index + "_cmd_" + cmdIndex, command, chance, "&eMigrated Command"));
                            cmdIndex++;
                        }
                    }
                }

                Object itemObj = rewardMap.get("item");
                if (itemObj instanceof Map<?, ?> map) {
                    ItemStack item = parseItemMap(map, file.getName() + " reward " + index);
                    if (item != null) {
                        subRewards.add(createItemReward(
                            id + "_reward_" + index + "_item",
                            item,
                            chance,
                            item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : "&eMigrated Item"
                        ));
                    }
                }

                if (subRewards.isEmpty()) {
                    warnings.add(file.getName() + ": skipped reward " + index + " because it had no supported data");
                    continue;
                }

                Reward reward = subRewards.size() == 1
                    ? subRewards.get(0)
                    : createMultiReward(id + "_reward_" + index, subRewards, chance, "&6Migrated Reward");
                reward.setChance(chance);
                rewards.add(reward);
                index++;
            }

            if (rewards.isEmpty()) {
                warnings.add(file.getName() + ": no importable rewards found");
                continue;
            }

            crates.add(createCrate(id, displayName, CrateType.RANDOM, animationType, rewards, defaultKeyItem(displayName)));
        }
        return crates;
    }
}
