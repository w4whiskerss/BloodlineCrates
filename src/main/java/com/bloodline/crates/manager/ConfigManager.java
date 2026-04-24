package com.bloodline.crates.manager;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.debug.DebugEventType;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.KeyMode;
import com.bloodline.crates.model.CrateType;
import com.bloodline.crates.model.Reward;
import com.bloodline.crates.reward.RewardType;
import com.bloodline.crates.util.RewardStorageUtil;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class ConfigManager {
    private final BloodlineCrates plugin;
    private final Map<String, Crate> crates = new HashMap<>();

    public ConfigManager(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    public void loadAll() throws InvalidConfigurationException {
        crates.clear();

        File cratesFolder = new File(plugin.getDataFolder(), "crates");
        if (!cratesFolder.exists()) {
            cratesFolder.mkdirs();
            plugin.saveResource("crates/example.yml", false);
        }

        File[] files = cratesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("No crate configurations found in crates/ folder");
            return;
        }

        for (File file : files) {
            try {
                loadCrate(file);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load crate from " + file.getName() + ": " + e.getMessage());
                throw new InvalidConfigurationException("Invalid crate configuration: " + file.getName(), e);
            }
        }

        plugin.getLogger().info("Loaded " + crates.size() + " crate(s)");
    }

    private void loadCrate(File file) throws InvalidConfigurationException {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String id = config.getString("id");
        if (id == null || id.isEmpty()) {
            throw new InvalidConfigurationException("Crate ID is missing or empty");
        }

        String displayName = color(config.getString("displayName", id));
        String description = color(config.getString("description", ""));

        CrateType type;
        try {
            type = CrateType.valueOf(config.getString("type", "RANDOM").toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidConfigurationException("Invalid crate type for " + id);
        }

        String keyId = config.getString("keyId", id + "_key");
        ItemStack keyDisplayItem = config.getItemStack("key.display-item");
        if (keyDisplayItem == null && config.contains("key.display-item")) {
            keyDisplayItem = parseLegacyItem(config.get("key.display-item"));
        }
        if (keyDisplayItem == null) {
            keyDisplayItem = defaultKeyItem(displayName, description);
        }
        int guiRows = config.getInt("adjustments.rows", 6);
        List<Integer> rewardSlots = new ArrayList<>(config.getIntegerList("adjustments.reward-slots"));

        boolean pityEnabled = config.getBoolean("pity.enabled", false);
        int pityThreshold = config.getInt("pity.threshold", 0);
        String jackpotRewardId = config.getString("pity.jackpot-reward-id");
        com.bloodline.crates.animation.AnimationConfig animationConfig = loadAnimationConfig(config);

        List<Reward> rewards = loadRewards(config, id);
        if (rewards.isEmpty()) {
            throw new InvalidConfigurationException("No valid rewards found for crate: " + id);
        }

        Crate crate = new Crate(
            id,
            displayName,
            type,
            keyId,
            rewards,
            animationConfig,
            pityEnabled,
            pityThreshold,
            jackpotRewardId,
            description,
            keyDisplayItem,
            guiRows,
            rewardSlots
        );
        crate.setHologramEnabled(config.getBoolean("hologram.enabled", true));
        List<String> hologramLines = config.getStringList("hologram.lines");
        if (hologramLines.isEmpty()) {
            hologramLines = List.of("&6{crate_name}", "&eRight-click to open", "&7Requires: &f{key_name}");
        }
        crate.setHologramLines(new ArrayList<>(hologramLines));
        crate.setCooldownEnabled(config.getBoolean("cooldown.enabled", false));
        crate.setCooldownDurationSeconds(config.getLong("cooldown.duration", 0L));
        crate.setCooldownBypassPermission(config.getString("cooldown.bypass-permission", "bloodcrates.cooldown.bypass"));
        crate.setCooldownMessage(config.getString("cooldown.message", "&cYou must wait &e{time} &cbefore opening this crate again."));
        crate.setKeyMode(resolveKeyMode(config));
        crate.setVirtualOnly(config.getBoolean("virtual-only", false));
        crate.setGlobalLimitEnabled(config.getBoolean("limits.global.enabled", false));
        crate.setGlobalLimitMax(config.getInt("limits.global.max", 0));
        crate.setGlobalLimitMessage(config.getString("limits.global.message", "&cThis crate has reached its global open limit!"));
        crate.setGlobalLimitOnReached(config.getString("limits.global.on-reached", "DISABLE"));
        crate.setPerPlayerLimitEnabled(config.getBoolean("limits.per-player.enabled", false));
        crate.setPerPlayerLimitMax(config.getInt("limits.per-player.max", 0));
        crate.setPerPlayerLimitMessage(config.getString("limits.per-player.message", "&cYou have reached the maximum opens for this crate."));
        crate.setPerPlayerResetInterval(config.getString("limits.per-player.reset-interval", "NEVER"));

        crates.put(id, crate);
    }

    private List<Reward> loadRewards(YamlConfiguration config, String crateId) throws InvalidConfigurationException {
        List<Reward> rewards = new ArrayList<>();
        if (!config.contains("rewards")) {
            throw new InvalidConfigurationException("No rewards defined for crate: " + crateId);
        }

        List<?> rewardsList = config.getList("rewards");
        if (rewardsList != null) {
            int index = 0;
            for (Object obj : rewardsList) {
                if (obj instanceof Map<?, ?> rewardMap) {
                    Reward reward = parseLegacyReward(rewardMap, crateId + "_reward_" + index);
                    if (reward != null) {
                        rewards.add(reward);
                    }
                }
                index++;
            }
            return rewards;
        }

        ConfigurationSection rewardsSection = config.getConfigurationSection("rewards");
        if (rewardsSection != null) {
            for (String key : rewardsSection.getKeys(false)) {
                ConfigurationSection rewardSection = rewardsSection.getConfigurationSection(key);
                if (rewardSection == null) {
                    continue;
                }
                Reward reward = RewardStorageUtil.readReward(rewardSection);
                if (reward == null) {
                    reward = parseLegacyRewardSection(rewardSection, key);
                }
                if (reward != null) {
                    rewards.add(reward);
                }
            }
        }
        return rewards;
    }

    private Reward parseLegacyReward(Map<?, ?> rewardMap, String defaultId) {
        try {
            RewardType rewardType = parseRewardType(rewardMap.get("type"), rewardMap.get("reward-type"));
            ItemStack item = parseLegacyItem(rewardMap.get("item"));
            ItemStack displayItem = parseLegacyItem(rewardMap.get("display-item"));
            if (displayItem == null) {
                displayItem = parseLegacyItem(rewardMap.get("displayItem"));
            }
            if (rewardType == RewardType.ITEM && item == null) {
                return null;
            }
            Object chanceValue = rewardMap.get("chance");
            double chance = chanceValue instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(chanceValue == null ? 1.0D : chanceValue));
            Object permissionValue = rewardMap.get("permission");
            String permission = permissionValue == null ? "" : String.valueOf(permissionValue);
            String command = rewardMap.containsKey("command") ? String.valueOf(rewardMap.get("command")) : null;
            String displayName = rewardMap.containsKey("display-name") ? String.valueOf(rewardMap.get("display-name")) : null;
            String description = rewardMap.containsKey("description") ? String.valueOf(rewardMap.get("description")) : null;
            Reward reward = new Reward(
                String.valueOf(rewardMap.containsKey("id") ? rewardMap.get("id") : defaultId),
                rewardType,
                item,
                chance,
                "null".equals(permission) ? "" : permission,
                rewardType == RewardType.ECONOMY,
                readEconomyAmount(rewardMap),
                command,
                displayName,
                description
            );
            reward.setDisplayItem(displayItem == null ? (item == null ? null : item.clone()) : displayItem);
            reward.setBroadcast(booleanValue(rewardMap.get("broadcast")));
            reward.setBroadcastMessage(stringValue(rewardMap.get("broadcast-message")));
            reward.setCrateId(stringValue(rewardMap.get("crate-id")));
            reward.setHidden(booleanValue(rewardMap.get("hidden")));
            reward.setSubRewards(readSubRewards(rewardMap.get("sub-rewards"), defaultId + "_sub"));
            return reward;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse reward: " + e.getMessage());
            return null;
        }
    }

    private Reward parseLegacyRewardSection(ConfigurationSection section, String defaultId) {
        try {
            Reward direct = RewardStorageUtil.readReward(section);
            if (direct != null) {
                return direct;
            }

            ItemStack item = parseLegacyItem(section.getConfigurationSection("item"));
            if (item == null && parseRewardType(section.getString("type"), section.getString("reward-type")) == RewardType.ITEM) {
                return null;
            }
            Reward reward = new Reward(
                section.getString("id", defaultId),
                parseRewardType(section.getString("type"), section.getString("reward-type")),
                item,
                section.getDouble("chance", 1.0D),
                section.getString("permission", ""),
                section.getBoolean("economy-reward.enabled", false) || section.getDouble("economy-amount", 0.0D) > 0.0D,
                section.getDouble("economy-amount", section.getDouble("economy-reward.amount", 0.0D)),
                section.getString("command"),
                section.getString("display-name"),
                section.getString("description")
            );
            reward.setDisplayItem(section.getItemStack("display-item"));
            reward.setBroadcast(section.getBoolean("broadcast", false));
            reward.setBroadcastMessage(section.getString("broadcast-message"));
            reward.setCrateId(section.getString("crate-id"));
            reward.setHidden(section.getBoolean("hidden", false));
            reward.setSubRewards(readSubRewards(section.getList("sub-rewards"), defaultId + "_sub"));
            return reward;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse reward section: " + e.getMessage());
            return null;
        }
    }

    private ItemStack parseLegacyItem(Object itemObj) {
        if (itemObj instanceof ItemStack itemStack) {
            return itemStack;
        }
        if (itemObj instanceof Map<?, ?> itemMap) {
            return parseItemFromMap(itemMap);
        }
        if (itemObj instanceof ConfigurationSection section) {
            return parseItemFromSection(section);
        }
        return null;
    }

    private ItemStack parseItemFromMap(Map<?, ?> itemMap) {
        String materialName = String.valueOf(itemMap.get("material"));
        Optional<XMaterial> xMat = XMaterial.matchXMaterial(materialName);
        if (xMat.isEmpty()) {
            plugin.getLogger().warning("Invalid material: " + materialName);
            return null;
        }

        ItemStack item = xMat.get().parseItem();
        if (item == null) {
            return null;
        }

        Object amountObj = itemMap.get("amount");
        if (amountObj instanceof Number number) {
            item.setAmount(number.intValue());
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (itemMap.containsKey("name")) {
                meta.setDisplayName(color(String.valueOf(itemMap.get("name"))));
            }
            if (itemMap.get("lore") instanceof List<?> loreList) {
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

    private ItemStack parseItemFromSection(ConfigurationSection section) {
        String materialName = section.getString("material");
        if (materialName == null) {
            return null;
        }

        Optional<XMaterial> xMat = XMaterial.matchXMaterial(materialName);
        if (xMat.isEmpty()) {
            plugin.getLogger().warning("Invalid material: " + materialName);
            return null;
        }

        ItemStack item = xMat.get().parseItem();
        if (item == null) {
            return null;
        }
        item.setAmount(section.getInt("amount", 1));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (section.contains("name")) {
                meta.setDisplayName(color(section.getString("name")));
            }
            if (section.contains("lore")) {
                List<String> lore = new ArrayList<>();
                for (String line : section.getStringList("lore")) {
                    lore.add(color(line));
                }
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack defaultKeyItem(String displayName, String description) {
        Optional<XMaterial> hook = XMaterial.matchXMaterial("TRIPWIRE_HOOK");
        ItemStack item = hook.map(XMaterial::parseItem).orElse(new ItemStack(org.bukkit.Material.TRIPWIRE_HOOK));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName + " §6Key");
            if (description != null && !description.isBlank()) {
                meta.setLore(Collections.singletonList(description));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private com.bloodline.crates.animation.AnimationConfig loadAnimationConfig(YamlConfiguration config) {
        com.bloodline.crates.animation.AnimationType type = com.bloodline.crates.animation.AnimationType.INSTANT;
        int durationTicks = 60;
        String particleType = "FLAME";
        int particleCount = 30;
        String openSound = "BLOCK_CHEST_OPEN";
        String revealSound = "ENTITY_PLAYER_LEVELUP";
        String skipPermission = "bloodcrates.skip";

        if (config.contains("animation")) {
            ConfigurationSection animSection = config.getConfigurationSection("animation");
            if (animSection != null) {
                try {
                    type = com.bloodline.crates.animation.AnimationType.valueOf(animSection.getString("type", "INSTANT").toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
                durationTicks = animSection.getInt("duration-ticks", 60);
                particleType = animSection.getString("particles.type", "FLAME");
                particleCount = animSection.getInt("particles.count", 30);
                openSound = animSection.getString("sounds.open", "BLOCK_CHEST_OPEN");
                revealSound = animSection.getString("sounds.reveal", "ENTITY_PLAYER_LEVELUP");
                skipPermission = animSection.getString("skip-permission", "bloodcrates.skip");
            }
        }

        return new com.bloodline.crates.animation.AnimationConfig(
            type, durationTicks, particleType, particleCount, openSound, revealSound, skipPermission
        );
    }

    public Optional<Crate> getCrate(String id) {
        return Optional.ofNullable(crates.get(id));
    }

    public Collection<Crate> getAllCrates() {
        return Collections.unmodifiableCollection(crates.values());
    }

    public String getPrefix() {
        return color(plugin.getConfig().getString("prefix", "&8[&6BloodlineCrates&8] &7"));
    }

    public String getMessage(String key) {
        return color(plugin.getConfig().getString("messages." + key, "&cMessage not found: " + key));
    }

    public void reload() throws InvalidConfigurationException {
        loadAll();
        plugin.getDebugManager().emit(DebugEventType.CONFIG_RELOAD, null,
            () -> "loaded " + crates.size() + " crate(s) from disk");
    }

    public void saveCrate(Crate crate) {
        try {
            File crateFile = new File(plugin.getDataFolder(), "crates/" + crate.getId() + ".yml");
            YamlConfiguration config = new YamlConfiguration();

            config.set("id", crate.getId());
            config.set("displayName", decolor(crate.getDisplayName()));
            config.set("description", decolor(crate.getDescription()));
            config.set("type", crate.getType().name());
            config.set("keyId", crate.getKeyId());
            config.set("key.display-item", crate.getKeyDisplayItem());
            config.set("adjustments.rows", crate.getGuiRowsClamped());
            config.set("adjustments.reward-slots", crate.getEffectiveRewardSlots());
            config.set("hologram.enabled", crate.isHologramEnabled());
            config.set("hologram.lines", crate.getHologramLines());
            config.set("cooldown.enabled", crate.isCooldownEnabled());
            config.set("cooldown.duration", crate.getCooldownDurationSeconds());
            config.set("cooldown.bypass-permission", crate.getCooldownBypassPermission());
            config.set("cooldown.message", crate.getCooldownMessage());
            config.set("key-mode", crate.getKeyMode().name());
            config.set("virtual-only", crate.isVirtualOnly());
            config.set("limits.global.enabled", crate.isGlobalLimitEnabled());
            config.set("limits.global.max", crate.getGlobalLimitMax());
            config.set("limits.global.message", crate.getGlobalLimitMessage());
            config.set("limits.global.on-reached", crate.getGlobalLimitOnReached());
            config.set("limits.per-player.enabled", crate.isPerPlayerLimitEnabled());
            config.set("limits.per-player.max", crate.getPerPlayerLimitMax());
            config.set("limits.per-player.message", crate.getPerPlayerLimitMessage());
            config.set("limits.per-player.reset-interval", crate.getPerPlayerResetInterval());
            config.set("pity.enabled", crate.isPityEnabled());
            config.set("pity.threshold", crate.getPityThreshold());
            config.set("pity.jackpot-reward-id", crate.getJackpotRewardId());

            saveAnimationConfig(config, crate.getAnimationConfig());

            for (int i = 0; i < crate.getRewards().size(); i++) {
                RewardStorageUtil.writeReward(config.createSection("rewards." + i), crate.getRewards().get(i));
            }

            config.save(crateFile);
            crates.put(crate.getId(), crate);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save crate " + crate.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveAnimationConfig(YamlConfiguration config, com.bloodline.crates.animation.AnimationConfig animConfig) {
        if (animConfig == null) {
            return;
        }
        config.set("animation.type", animConfig.getType().name());
        config.set("animation.duration-ticks", animConfig.getDurationTicks());
        config.set("animation.particles.type", animConfig.getParticleType());
        config.set("animation.particles.count", animConfig.getParticleCount());
        config.set("animation.sounds.open", animConfig.getOpenSound());
        config.set("animation.sounds.reveal", animConfig.getRevealSound());
        config.set("animation.skip-permission", animConfig.getSkipPermission());
    }

    private KeyMode resolveKeyMode(YamlConfiguration config) {
        String raw = config.getString("key-mode");
        if (raw != null && !raw.isBlank()) {
            try {
                return KeyMode.valueOf(raw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return config.getBoolean("virtual-only", false) ? KeyMode.VIRTUAL : KeyMode.PHYSICAL;
    }

    private RewardType parseRewardType(Object primary, Object fallback) {
        String raw = primary != null ? String.valueOf(primary) : String.valueOf(fallback == null ? "ITEM" : fallback);
        try {
            return RewardType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return RewardType.ITEM;
        }
    }

    private double readEconomyAmount(Map<?, ?> rewardMap) {
        Object direct = rewardMap.get("economy-amount");
        if (direct instanceof Number number) {
            return number.doubleValue();
        }
        Object economy = rewardMap.get("economy-reward");
        if (economy instanceof Map<?, ?> economyMap) {
            Object amount = economyMap.get("amount");
            if (amount instanceof Number number) {
                return number.doubleValue();
            }
        }
        return direct == null ? 0.0D : Double.parseDouble(String.valueOf(direct));
    }

    private List<Reward> readSubRewards(Object rawSubRewards, String defaultPrefix) {
        List<Reward> rewards = new ArrayList<>();
        if (rawSubRewards instanceof List<?> list) {
            int index = 0;
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> map) {
                    Reward reward = parseLegacyReward(map, defaultPrefix + "_" + index);
                    if (reward != null) {
                        rewards.add(reward);
                    }
                }
                index++;
            }
        }
        return rewards;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean booleanValue(Object value) {
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    public void deleteCrate(String crateId) {
        try {
            File crateFile = new File(plugin.getDataFolder(), "crates/" + crateId + ".yml");
            if (crateFile.exists()) {
                crateFile.delete();
            }
            crates.remove(crateId);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to delete crate " + crateId + ": " + e.getMessage());
        }
    }

    private String color(String input) {
        return input == null ? "" : ChatColor.translateAlternateColorCodes('&', input);
    }

    private String decolor(String input) {
        return input == null ? "" : input.replace("§", "&");
    }
}
