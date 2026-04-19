package com.bloodlinecrates.manager;

import com.bloodlinecrates.BloodlineCratesPlugin;
import com.bloodlinecrates.api.CrateOpenEvent;
import com.bloodlinecrates.api.RewardGiveEvent;
import com.bloodlinecrates.model.AnimationType;
import com.bloodlinecrates.model.CooldownScope;
import com.bloodlinecrates.model.CrateDefinition;
import com.bloodlinecrates.model.CrateLocation;
import com.bloodlinecrates.model.CrateReward;
import com.bloodlinecrates.model.CrateType;
import com.bloodlinecrates.model.EffectSettings;
import com.bloodlinecrates.model.PlayerProfile;
import com.bloodlinecrates.model.RewardTier;
import com.bloodlinecrates.util.Text;
import com.bloodlinecrates.util.TimeUtil;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class CrateManager {
    private final JavaPlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final KeyManager keyManager;
    private final AnalyticsManager analyticsManager;
    private final EffectManager effectManager;
    private final AntiExploitManager antiExploitManager;
    private final Map<String, CrateDefinition> crates = new HashMap<>();

    public CrateManager(JavaPlugin plugin,
                        PlayerDataManager playerDataManager,
                        KeyManager keyManager,
                        AnalyticsManager analyticsManager,
                        EffectManager effectManager,
                        AntiExploitManager antiExploitManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.keyManager = keyManager;
        this.analyticsManager = analyticsManager;
        this.effectManager = effectManager;
        this.antiExploitManager = antiExploitManager;
    }

    public void load(FileConfiguration configuration) {
        crates.clear();
        ConfigurationSection section = configuration.getConfigurationSection("crates");
        if (section == null) {
            return;
        }
        for (String crateId : section.getKeys(false)) {
            ConfigurationSection crateSection = section.getConfigurationSection(crateId);
            if (crateSection == null) {
                continue;
            }
            CrateDefinition crate = new CrateDefinition(crateId);
            crate.setDisplayName(crateSection.getString("display_name", crateId));
            crate.setType(CrateType.valueOf(crateSection.getString("type", "RNG")));
            crate.setKeyId(crateSection.getString("key", ""));
            crate.setItemsAdderModel(crateSection.getString("itemsadder_model", ""));
            crate.setPreviewChances(crateSection.getBoolean("preview_chances", true));

            ConfigurationSection cooldown = crateSection.getConfigurationSection("cooldown");
            if (cooldown != null) {
                crate.setCooldownScope(CooldownScope.valueOf(cooldown.getString("scope", "PER_CRATE")));
                crate.setCooldownMillis(cooldown.getLong("seconds", 0L) * 1000L);
            }

            ConfigurationSection pity = crateSection.getConfigurationSection("pity");
            if (pity != null) {
                crate.setPityEnabled(pity.getBoolean("enabled", false));
                crate.setPityThreshold(pity.getInt("threshold", 0));
                crate.setPityGuaranteedTier(RewardTier.valueOf(pity.getString("guaranteed_tier", "RARE")));
            }

            ConfigurationSection animation = crateSection.getConfigurationSection("animation");
            if (animation != null) {
                crate.setEffectSettings(new EffectSettings(
                        AnimationType.valueOf(animation.getString("type", "SPIN")),
                        animation.getInt("speed_ticks", 2),
                        animation.getInt("duration_ticks", 40),
                        animation.getBoolean("particles", true),
                        animation.getBoolean("sounds", true),
                        animation.getBoolean("synced", true),
                        animation.getString("skip_permission", "bloodlinecrates.skipanimation")
                ));
            }

            for (Map<?, ?> rawLocation : crateSection.getMapList("locations")) {
                MemoryConfiguration temp = new MemoryConfiguration();
                rawLocation.forEach((key, value) -> temp.set(String.valueOf(key), value));
                crate.locations().add(CrateLocation.fromConfig(temp));
            }

            ConfigurationSection bundles = crateSection.getConfigurationSection("bundles");
            if (bundles != null) {
                for (String threshold : bundles.getKeys(false)) {
                    crate.bundleCommands().put(Integer.parseInt(threshold), bundles.getStringList(threshold + ".commands"));
                }
            }

            ConfigurationSection rewards = crateSection.getConfigurationSection("rewards");
            if (rewards != null) {
                for (String rewardId : rewards.getKeys(false)) {
                    ConfigurationSection rewardSection = rewards.getConfigurationSection(rewardId);
                    if (rewardSection == null) {
                        continue;
                    }
                    CrateReward reward = new CrateReward(rewardId);
                    reward.setTier(RewardTier.valueOf(rewardSection.getString("tier", "COMMON")));
                    reward.setChance(rewardSection.getDouble("chance", 100.0D));
                    reward.setPermission(rewardSection.getString("permission", ""));
                    reward.setBroadcast(rewardSection.getBoolean("broadcast", false));
                    reward.setTitle(rewardSection.getBoolean("title", false));
                    reward.setBossbar(rewardSection.getBoolean("bossbar", false));
                    reward.setDuplicateProtection(rewardSection.getBoolean("duplicate_protection", false));
                    reward.setWeightGroup(rewardSection.getString("weight_group", "default"));
                    reward.setItem(buildRewardItem(rewardSection.getConfigurationSection("item"), rewardId));
                    reward.commands().addAll(rewardSection.getStringList("commands"));
                    crate.rewards().put(reward.id(), reward);
                }
            }
            crates.put(crate.id(), crate);
        }
    }

    public Optional<CrateDefinition> get(String id) {
        return Optional.ofNullable(crates.get(id.toLowerCase()));
    }

    public Collection<CrateDefinition> all() {
        return crates.values();
    }

    public void register(CrateDefinition crate) {
        crates.put(crate.id(), crate);
    }

    public void delete(String crateId) {
        crates.remove(crateId.toLowerCase());
    }

    public Optional<CrateDefinition> findByLocation(Location location) {
        return crates.values().stream().filter(crate -> crate.locations().stream().anyMatch(crateLocation -> {
            Location stored = crateLocation.toBukkitLocation();
            return stored != null && stored.getWorld() == location.getWorld() && stored.distanceSquared(location) <= 1.5D;
        })).findFirst();
    }

    public List<CrateReward> sortedRewards(CrateDefinition crate) {
        return crate.rewards().values().stream()
                .sorted(Comparator.comparing(CrateReward::tier).thenComparing(CrateReward::id))
                .toList();
    }

    public void open(Player player, CrateDefinition crate, boolean testMode, int amount) {
        if (antiExploitManager.isBlocked(player)) {
            return;
        }
        CrateOpenEvent event = new CrateOpenEvent(player, crate, testMode);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        PlayerProfile profile = playerDataManager.getOrLoad(player.getUniqueId());
        long now = System.currentTimeMillis();
        long cooldownUntil = crate.cooldownScope() == CooldownScope.GLOBAL
                ? profile.globalCooldownUntil()
                : profile.crateCooldowns().getOrDefault(crate.id(), 0L);
        if (cooldownUntil > now) {
            player.sendMessage(Text.color(((BloodlineCratesPlugin) plugin).messages().getString("cooldown"))
                    .replace("%time%", TimeUtil.formatMillis(cooldownUntil - now)));
            return;
        }

        if (!testMode && !consumeKey(player, crate, amount)) {
            player.sendMessage(Text.color("&cYou do not have enough keys."));
            return;
        }

        antiExploitManager.markOpening(player);
        if (crate.type() == CrateType.SELECTABLE) {
            ((BloodlineCratesPlugin) plugin).guiManager().openSelectableCrate(player, crate, testMode);
            return;
        }

        for (int i = 0; i < amount; i++) {
            CrateReward reward = rollReward(player, crate);
            giveReward(player, crate, reward);
        }

        effectManager.playOpenEffects(player, crate);
        finalizeOpen(player, crate, profile, amount);
    }

    public void finalizeSelectableReward(Player player, CrateDefinition crate, CrateReward reward, boolean testMode, int amount) {
        for (int i = 0; i < amount; i++) {
            giveReward(player, crate, reward);
        }
        effectManager.playOpenEffects(player, crate);
        finalizeOpen(player, crate, playerDataManager.getOrLoad(player.getUniqueId()), amount);
        if (testMode) {
            player.sendMessage(Text.color(((BloodlineCratesPlugin) plugin).messages().getString("test-mode")));
        }
    }

    private boolean consumeKey(Player player, CrateDefinition crate, int amount) {
        if (crate.keyId() == null || crate.keyId().isBlank()) {
            return true;
        }
        int virtualKeys = keyManager.getVirtual(player.getUniqueId(), crate.keyId());
        if (virtualKeys >= amount) {
            return keyManager.consumeVirtual(player.getUniqueId(), crate.keyId(), amount);
        }

        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !keyManager.isMatchingPhysicalKey(item, crate.keyId())) {
                continue;
            }
            int take = Math.min(item.getAmount(), remaining);
            item.setAmount(item.getAmount() - take);
            remaining -= take;
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private CrateReward rollReward(Player player, CrateDefinition crate) {
        PlayerProfile profile = playerDataManager.getOrLoad(player.getUniqueId());
        int pity = profile.pityCounters().getOrDefault(crate.id(), 0) + 1;
        profile.pityCounters().put(crate.id(), pity);
        if (crate.pityEnabled() && pity >= crate.pityThreshold()) {
            List<CrateReward> pityPool = crate.rewards().values().stream()
                    .filter(reward -> reward.tier().ordinal() >= crate.pityGuaranteedTier().ordinal())
                    .toList();
            if (!pityPool.isEmpty()) {
                profile.pityCounters().put(crate.id(), 0);
                return pityPool.get(ThreadLocalRandom.current().nextInt(pityPool.size()));
            }
        }

        List<CrateReward> allowed = crate.rewards().values().stream()
                .filter(reward -> reward.permission().isBlank() || player.hasPermission(reward.permission()))
                .toList();
        double total = allowed.stream().mapToDouble(CrateReward::chance).sum();
        double roll = ThreadLocalRandom.current().nextDouble(Math.max(total, 1.0D));
        double cursor = 0.0D;
        for (CrateReward reward : allowed) {
            cursor += reward.chance();
            if (roll <= cursor) {
                return reward;
            }
        }
        return allowed.isEmpty() ? crate.rewards().values().iterator().next() : allowed.getFirst();
    }

    private void giveReward(Player player, CrateDefinition crate, CrateReward reward) {
        if (!reward.permission().isBlank() && !player.hasPermission(reward.permission())) {
            player.sendMessage(Text.color(((BloodlineCratesPlugin) plugin).messages().getString("reward-denied")));
            return;
        }
        RewardGiveEvent event = new RewardGiveEvent(player, crate, reward);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        if (reward.item() != null && reward.item().getType() != Material.AIR) {
            player.getInventory().addItem(reward.item().clone());
        }
        reward.commands().forEach(command ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName())));
        if (reward.broadcast()) {
            Bukkit.broadcast(Text.component("&6" + player.getName() + " won " + reward.id() + " from " + crate.displayName()));
        }
        if (reward.title()) {
            player.showTitle(Title.title(Text.component("&6Reward Claimed"), Text.component("&f" + reward.id())));
        }
        analyticsManager.recordReward(player, crate, reward);
        PlayerProfile profile = playerDataManager.getOrLoad(player.getUniqueId());
        profile.setLastReward(reward.id());
    }

    private void finalizeOpen(Player player, CrateDefinition crate, PlayerProfile profile, int amount) {
        long now = System.currentTimeMillis();
        if (crate.cooldownScope() == CooldownScope.GLOBAL) {
            profile.setGlobalCooldownUntil(now + crate.cooldownMillis());
        } else {
            profile.crateCooldowns().put(crate.id(), now + crate.cooldownMillis());
        }
        for (int i = 0; i < amount; i++) {
            profile.incrementTotalCratesOpened();
        }
        profile.setLastOpen(Instant.now());
        analyticsManager.recordOpen(player, crate, amount);
        playerDataManager.save(profile);
    }

    private ItemStack buildRewardItem(ConfigurationSection itemSection, String rewardId) {
        if (itemSection == null) {
            return new ItemStack(Material.AIR);
        }
        Material material = Material.matchMaterial(itemSection.getString("material", "CHEST"));
        ItemStack item = new ItemStack(material == null ? Material.CHEST : material, itemSection.getInt("amount", 1));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.component(itemSection.getString("name", rewardId)));
        meta.lore(Text.color(itemSection.getStringList("lore")).stream().map(Text::component).toList());
        item.setItemMeta(meta);
        return item;
    }
}
