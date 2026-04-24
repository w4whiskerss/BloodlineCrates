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

public class CrateReloadedImporter extends AbstractCrateImporter {
    @Override
    public MigrationSource getSource() {
        return MigrationSource.CRATE_RELOADED;
    }

    @Override
    public List<Crate> importCrates(File sourceDirectory) {
        resetMessages();
        if (requireDirectory(sourceDirectory) == null) {
            return List.of();
        }

        List<Crate> crates = new ArrayList<>();
        for (File file : findYamlFiles(sourceDirectory)) {
            YamlConfiguration config = load(file);
            String id = stringValue(config.getString("crate.id"), baseId(file)).toLowerCase();
            String displayName = stringValue(config.getString("crate.name"), id);
            String rawType = stringValue(config.getString("crate.type"), "ROULETTE");
            CrateType crateType = "CSGO".equalsIgnoreCase(rawType) ? CrateType.SELECTABLE : CrateType.RANDOM;
            AnimationType animationType = crateType == CrateType.SELECTABLE ? AnimationType.CSGO : AnimationType.ROULETTE;

            List<Reward> rewards = new ArrayList<>();
            ConfigurationSection prizes = config.getConfigurationSection("prizes");
            List<Map<String, Object>> entries = prizes != null ? sectionChildren(prizes) : List.of();
            if (entries.isEmpty()) {
                warnings.add(file.getName() + ": no prizes found");
                continue;
            }

            int index = 0;
            for (Map<String, Object> prize : entries) {
                double chance = doubleValue(prize.get("chance"), 0.0D);
                List<Reward> subRewards = new ArrayList<>();

                Object commands = prize.get("commands");
                if (commands instanceof List<?> commandList) {
                    int commandIndex = 0;
                    for (Object commandObj : commandList) {
                        String command = sanitizeCommand(String.valueOf(commandObj));
                        if (command != null && !command.isBlank()) {
                            subRewards.add(createCommandReward(id + "_reward_" + index + "_cmd_" + commandIndex, command, chance, "&eMigrated Command"));
                            commandIndex++;
                        }
                    }
                }

                Object items = prize.get("items");
                if (items instanceof List<?> itemList) {
                    int itemIndex = 0;
                    for (Object itemObj : itemList) {
                        if (itemObj instanceof Map<?, ?> itemMap) {
                            ItemStack item = parseItemMap(itemMap, file.getName() + " prize " + index);
                            if (item != null) {
                                subRewards.add(createItemReward(
                                    id + "_reward_" + index + "_item_" + itemIndex,
                                    item,
                                    chance,
                                    item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : "&eMigrated Item"
                                ));
                                itemIndex++;
                            }
                        }
                    }
                }

                if (subRewards.isEmpty()) {
                    warnings.add(file.getName() + ": skipped prize " + index + " because it had no supported items or commands");
                    continue;
                }

                Reward reward = subRewards.size() == 1
                    ? subRewards.get(0)
                    : createMultiReward(id + "_reward_" + index, subRewards, chance, "&6Migrated Prize");
                reward.setChance(chance);
                rewards.add(reward);
                index++;
            }

            if (rewards.isEmpty()) {
                warnings.add(file.getName() + ": no importable rewards found");
                continue;
            }

            crates.add(createCrate(id, displayName, crateType, animationType, rewards, defaultKeyItem(displayName)));
        }
        return crates;
    }
}
