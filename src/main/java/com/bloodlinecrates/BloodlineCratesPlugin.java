package com.bloodlinecrates;

import com.bloodlinecrates.command.BloodlineCommand;
import com.bloodlinecrates.gui.GuiManager;
import com.bloodlinecrates.hook.BloodlinePlaceholderExpansion;
import com.bloodlinecrates.listener.CrateListener;
import com.bloodlinecrates.manager.AnalyticsManager;
import com.bloodlinecrates.manager.AntiExploitManager;
import com.bloodlinecrates.manager.AuditLogManager;
import com.bloodlinecrates.manager.CrateManager;
import com.bloodlinecrates.manager.EffectManager;
import com.bloodlinecrates.manager.KeyManager;
import com.bloodlinecrates.manager.PlayerDataManager;
import com.bloodlinecrates.model.CrateDefinition;
import com.bloodlinecrates.model.CrateKey;
import com.bloodlinecrates.model.CrateLocation;
import com.bloodlinecrates.model.CrateReward;
import com.bloodlinecrates.model.TimedKeyRule;
import com.bloodlinecrates.storage.MySqlStorageProvider;
import com.bloodlinecrates.storage.StorageProvider;
import com.bloodlinecrates.storage.YamlStorageProvider;
import com.bloodlinecrates.util.ItemStackSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class BloodlineCratesPlugin extends JavaPlugin {
    private static NamespacedKey KEY_ID_KEY;

    private FileConfiguration cratesConfig;
    private FileConfiguration keysConfig;
    private FileConfiguration messages;
    private StorageProvider storageProvider;
    private PlayerDataManager playerDataManager;
    private KeyManager keyManager;
    private AnalyticsManager analyticsManager;
    private EffectManager effectManager;
    private AntiExploitManager antiExploitManager;
    private CrateManager crateManager;
    private AuditLogManager auditLogManager;
    private GuiManager guiManager;
    private BloodlineCommand commandHandler;

    public static NamespacedKey keyIdKey() {
        return KEY_ID_KEY;
    }

    @Override
    public void onEnable() {
        try {
            Class.forName(ItemStackSerializer.class.getName());
        } catch (ClassNotFoundException ignored) {
        }
        KEY_ID_KEY = new NamespacedKey(this, "key-id");
        saveDefaultConfig();
        saveMissing("crates.yml");
        saveMissing("keys.yml");
        saveMissing("messages.yml");
        reloadPlugin();
        getServer().getPluginManager().registerEvents(new CrateListener(this), this);
        commandHandler = new BloodlineCommand(this);
        getCommand("bloodcrates").setExecutor(commandHandler);
        getCommand("bloodcrates").setTabCompleter(commandHandler);
        long interval = getConfig().getLong("timed_keys.check_interval_seconds", 30L) * 20L;
        getServer().getScheduler().runTaskTimer(this, () -> keyManager.grantTimedKeys(), interval, interval);
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new BloodlinePlaceholderExpansion(this).register();
        }
    }

    @Override
    public void onDisable() {
        saveAllData();
        try {
            if (storageProvider != null) {
                storageProvider.shutdown();
            }
        } catch (Exception exception) {
            getLogger().warning("Failed to close storage: " + exception.getMessage());
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        cratesConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "crates.yml"));
        keysConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "keys.yml"));
        messages = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));

        try {
            storageProvider = createStorage();
            storageProvider.initialize();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize storage", exception);
        }

        playerDataManager = new PlayerDataManager(this, storageProvider);
        try {
            playerDataManager.loadAll();
        } catch (Exception exception) {
            getLogger().warning("Failed to load all profiles: " + exception.getMessage());
        }
        analyticsManager = new AnalyticsManager(this);
        effectManager = new EffectManager(this);
        antiExploitManager = new AntiExploitManager(this);
        keyManager = new KeyManager(this, playerDataManager);
        keyManager.load(keysConfig);
        crateManager = new CrateManager(this, playerDataManager, keyManager, analyticsManager, effectManager, antiExploitManager);
        crateManager.load(cratesConfig);
        auditLogManager = new AuditLogManager(this);
        guiManager = new GuiManager(this);
    }

    public void saveAllData() {
        playerDataManager.saveAll();
        saveCrates();
        saveKeys();
    }

    private void saveCrates() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (CrateDefinition crate : crateManager.all()) {
            String path = "crates." + crate.id();
            yaml.set(path + ".display_name", crate.displayName());
            yaml.set(path + ".type", crate.type().name());
            yaml.set(path + ".key", crate.keyId());
            yaml.set(path + ".itemsadder_model", crate.itemsAdderModel());
            yaml.set(path + ".preview_chances", crate.previewChances());
            yaml.set(path + ".cooldown.scope", crate.cooldownScope().name());
            yaml.set(path + ".cooldown.seconds", crate.cooldownMillis() / 1000L);
            yaml.set(path + ".pity.enabled", crate.pityEnabled());
            yaml.set(path + ".pity.threshold", crate.pityThreshold());
            yaml.set(path + ".pity.guaranteed_tier", crate.pityGuaranteedTier().name());
            yaml.set(path + ".animation.type", crate.effectSettings().animationType().name());
            yaml.set(path + ".animation.speed_ticks", crate.effectSettings().speedTicks());
            yaml.set(path + ".animation.duration_ticks", crate.effectSettings().durationTicks());
            yaml.set(path + ".animation.particles", crate.effectSettings().particles());
            yaml.set(path + ".animation.sounds", crate.effectSettings().sounds());
            yaml.set(path + ".animation.synced", crate.effectSettings().synced());
            yaml.set(path + ".animation.skip_permission", crate.effectSettings().skipPermission());
            int locationIndex = 0;
            for (CrateLocation location : crate.locations()) {
                yaml.set(path + ".locations." + locationIndex + ".world", location.world());
                yaml.set(path + ".locations." + locationIndex + ".x", location.x());
                yaml.set(path + ".locations." + locationIndex + ".y", location.y());
                yaml.set(path + ".locations." + locationIndex + ".z", location.z());
                yaml.set(path + ".locations." + locationIndex + ".yaw", location.yaw());
                yaml.set(path + ".locations." + locationIndex + ".pitch", location.pitch());
                locationIndex++;
            }
            for (CrateReward reward : crate.rewards().values()) {
                String rewardPath = path + ".rewards." + reward.id();
                yaml.set(rewardPath + ".tier", reward.tier().name());
                yaml.set(rewardPath + ".chance", reward.chance());
                yaml.set(rewardPath + ".permission", reward.permission());
                yaml.set(rewardPath + ".broadcast", reward.broadcast());
                yaml.set(rewardPath + ".title", reward.title());
                yaml.set(rewardPath + ".bossbar", reward.bossbar());
                yaml.set(rewardPath + ".duplicate_protection", reward.duplicateProtection());
                yaml.set(rewardPath + ".weight_group", reward.weightGroup());
                if (reward.item() != null && reward.item().getType() != Material.AIR) {
                    yaml.set(rewardPath + ".item", reward.item());
                }
                yaml.set(rewardPath + ".commands", reward.commands());
            }
        }
        saveYaml(yaml, "crates.yml");
    }

    private void saveKeys() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (CrateKey key : keyManager.all()) {
            String path = "keys." + key.id();
            yaml.set(path + ".display_name", key.displayName());
            yaml.set(path + ".lore", key.lore());
            yaml.set(path + ".itemsadder_model", key.itemsAdderModel());
            yaml.set(path + ".physical_enabled", key.physicalEnabled());
            yaml.set(path + ".virtual_enabled", key.virtualEnabled());
            yaml.set(path + ".item", key.item());
            int index = 0;
            for (TimedKeyRule rule : key.timedKeyRules()) {
                String rulePath = path + ".timed_rewards." + index++;
                yaml.set(rulePath + ".interval_minutes", rule.intervalMinutes());
                yaml.set(rulePath + ".amount", rule.amount());
                yaml.set(rulePath + ".permission_multiplier", rule.permissionMultiplier());
            }
        }
        saveYaml(yaml, "keys.yml");
    }

    private void saveYaml(FileConfiguration yaml, String fileName) {
        try {
            yaml.save(new File(getDataFolder(), fileName));
        } catch (IOException exception) {
            getLogger().warning("Failed to save " + fileName + ": " + exception.getMessage());
        }
    }

    private StorageProvider createStorage() {
        String type = getConfig().getString("storage.type", "YAML");
        if (type.equalsIgnoreCase("MYSQL")) {
            ConfigurationSection mysql = getConfig().getConfigurationSection("storage.mysql");
            return new MySqlStorageProvider(this, mysql);
        }
        return new YamlStorageProvider(this);
    }

    private void saveMissing(String fileName) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists()) {
            saveResource(fileName, false);
        }
    }

    public FileConfiguration messages() {
        return messages;
    }

    public PlayerDataManager playerDataManager() {
        return playerDataManager;
    }

    public KeyManager keyManager() {
        return keyManager;
    }

    public AnalyticsManager analyticsManager() {
        return analyticsManager;
    }

    public CrateManager crateManager() {
        return crateManager;
    }

    public AuditLogManager auditLogManager() {
        return auditLogManager;
    }

    public GuiManager guiManager() {
        return guiManager;
    }

    public BloodlineCommand commandHandler() {
        return commandHandler;
    }
}
