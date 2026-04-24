package com.bloodline.crates;

import com.bloodline.crates.animation.AnimationManager;
import com.bloodline.crates.audit.AuditLogger;
import com.bloodline.crates.bundle.BundleManager;
import com.bloodline.crates.broadcast.BroadcastManager;
import com.bloodline.crates.command.BloodCratesCommand;
import com.bloodline.crates.cooldown.CooldownManager;
import com.bloodline.crates.debug.DebugEventType;
import com.bloodline.crates.discord.DiscordBotManager;
import com.bloodline.crates.debug.DebugManager;
import com.bloodline.crates.economy.VaultEconomyHook;
import com.bloodline.crates.failsafe.ClaimsManager;
import com.bloodline.crates.filter.WorldBlacklist;
import com.bloodline.crates.gui.GUIListener;
import com.bloodline.crates.hook.DiscordWebhookManager;
import com.bloodline.crates.leaderboard.LeaderboardManager;
import com.bloodline.crates.listener.ChatInputListener;
import com.bloodline.crates.loadout.LoadoutManager;
import com.bloodline.crates.limit.LimitManager;
import com.bloodline.crates.limit.LimitStore;
import com.bloodline.crates.migration.MigrationManager;
import com.bloodline.crates.manager.ConfigManager;
import com.bloodline.crates.manager.CrateManager;
import com.bloodline.crates.manager.GUIManager;
import com.bloodline.crates.manager.KeyManager;
import com.bloodline.crates.manager.PlayerDataManager;
import com.bloodline.crates.pity.PityManager;
import com.bloodline.crates.placeholder.CratesPlaceholderExpansion;
import com.bloodline.crates.placement.HologramHandler;
import com.bloodline.crates.placement.PlacedCrateStore;
import com.bloodline.crates.placement.PlacementManager;
import com.bloodline.crates.security.AntiAltManager;
import com.bloodline.crates.security.AntiDupeManager;
import com.bloodline.crates.sync.SyncManager;
import com.bloodline.crates.sync.impl.MySQLSyncManager;
import com.bloodline.crates.sync.impl.RedisSyncManager;
import com.bloodline.crates.stats.StatsGUI;
import com.bloodline.crates.stats.StatsManager;
import com.bloodline.crates.timed.TimedKeyManager;
import com.bloodline.crates.virtual.VirtualInventoryManager;
import com.bloodline.crates.virtual.VirtualInventoryStore;
import com.bloodline.crates.reward.RewardExecutorRegistry;
import com.bloodline.crates.reward.executor.BroadcastRewardExecutor;
import com.bloodline.crates.reward.executor.CommandRewardExecutor;
import com.bloodline.crates.reward.executor.CrateRewardExecutor;
import com.bloodline.crates.reward.executor.EconomyRewardExecutor;
import com.bloodline.crates.reward.executor.ItemRewardExecutor;
import com.bloodline.crates.reward.executor.MultiRewardExecutor;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class BloodlineCrates extends JavaPlugin {
    private ConfigManager configManager;
    private KeyManager keyManager;
    private PlayerDataManager playerDataManager;
    private GUIManager guiManager;
    private CrateManager crateManager;
    private AnimationManager animationManager;
    private TimedKeyManager timedKeyManager;
    private LeaderboardManager leaderboardManager;
    private ClaimsManager claimsManager;
    private AntiDupeManager antiDupeManager;
    private AntiAltManager antiAltManager;
    private BundleManager bundleManager;
    private WorldBlacklist worldBlacklist;
    private DiscordWebhookManager discordWebhookManager;
    private PityManager pityManager;
    private VaultEconomyHook vaultEconomyHook;
    private AuditLogger auditLogger;
    private SyncManager syncManager;
    private LoadoutManager loadoutManager;
    private PlacedCrateStore placedCrateStore;
    private HologramHandler hologramHandler;
    private PlacementManager placementManager;
    private CooldownManager cooldownManager;
    private RewardExecutorRegistry rewardExecutorRegistry;
    private VirtualInventoryStore virtualInventoryStore;
    private VirtualInventoryManager virtualInventoryManager;
    private BroadcastManager broadcastManager;
    private LimitStore limitStore;
    private LimitManager limitManager;
    private MigrationManager migrationManager;
    private DebugManager debugManager;
    private StatsManager statsManager;
    private StatsGUI statsGUI;
    private DiscordBotManager discordBotManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getDataFolder().mkdirs();

        this.configManager = new ConfigManager(this);
        this.keyManager = new KeyManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.guiManager = new GUIManager(this);
        this.animationManager = new AnimationManager(this);
        this.crateManager = new CrateManager(this);
        this.leaderboardManager = new LeaderboardManager(this);
        this.timedKeyManager = new TimedKeyManager(this);
        this.claimsManager = new ClaimsManager(this);
        this.antiDupeManager = new AntiDupeManager(this);
        this.antiAltManager = new AntiAltManager(this);
        this.bundleManager = new BundleManager(this);
        this.worldBlacklist = new WorldBlacklist(this);
        this.discordWebhookManager = new DiscordWebhookManager(this);
        this.pityManager = new PityManager(this);
        this.vaultEconomyHook = new VaultEconomyHook(this);
        this.auditLogger = new AuditLogger(this);
        this.loadoutManager = new LoadoutManager(this);
        this.placedCrateStore = new PlacedCrateStore(this);
        this.hologramHandler = new HologramHandler(this);
        this.cooldownManager = new CooldownManager(this);
        this.virtualInventoryStore = new VirtualInventoryStore(this);
        this.virtualInventoryManager = new VirtualInventoryManager(this, this.virtualInventoryStore);
        this.broadcastManager = new BroadcastManager(this);
        this.limitStore = new LimitStore(this);
        this.limitManager = new LimitManager(this, this.limitStore);
        this.migrationManager = new MigrationManager(this);
        this.debugManager = new DebugManager(this);
        this.statsManager = new StatsManager(this);
        this.statsGUI = new StatsGUI(this);
        this.discordBotManager = new DiscordBotManager(this);
        this.rewardExecutorRegistry = new RewardExecutorRegistry(this);
        this.rewardExecutorRegistry.register(new ItemRewardExecutor());
        this.rewardExecutorRegistry.register(new CommandRewardExecutor());
        this.rewardExecutorRegistry.register(new BroadcastRewardExecutor());
        this.rewardExecutorRegistry.register(new CrateRewardExecutor());
        this.rewardExecutorRegistry.register(new MultiRewardExecutor());
        if (this.vaultEconomyHook != null && this.vaultEconomyHook.isAvailable()) {
            this.rewardExecutorRegistry.register(new EconomyRewardExecutor());
        }

        try {
            this.configManager.loadAll();
        } catch (Exception e) {
            getLogger().severe("Failed to load configurations: " + e.getMessage());
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.placementManager = new PlacementManager(this, this.placedCrateStore, this.hologramHandler);

        this.syncManager = createSyncManager();
        if (this.syncManager != null) {
            this.syncManager.subscribeToUpdates(payload -> {
                getLogger().info("Received sync event: " + payload.getEventType() + " from " + payload.getServerId());
                this.debugManager.emit(DebugEventType.SYNC_EVENT, extractSubject(payload.getPayload()),
                    () -> "received " + payload.getEventType() + " from " + payload.getServerId() + " payload=" + payload.getPayload());
            });
        }

        PluginCommand command = getCommand("bloodcrates");
        if (command != null) {
            BloodCratesCommand bloodCratesCommand = new BloodCratesCommand(this);
            command.setExecutor(bloodCratesCommand);
            command.setTabCompleter(bloodCratesCommand);
        }

        Bukkit.getPluginManager().registerEvents(new GUIListener(this.guiManager), this);
        Bukkit.getPluginManager().registerEvents(new com.bloodline.crates.placement.listener.CrateInteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new com.bloodline.crates.placement.listener.CrateProtectListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatInputListener(this), this);
        Bukkit.getPluginManager().registerEvents(this.antiAltManager, this);
        Bukkit.getPluginManager().registerEvents(this.virtualInventoryStore, this);
        Bukkit.getPluginManager().registerEvents(this.debugManager, this);
        Bukkit.getPluginManager().registerEvents(this.statsGUI, this);

        this.antiDupeManager.recoverPendingTransactions();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new CratesPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered");
        }

        if (this.discordBotManager != null) {
            this.discordBotManager.start();
        }

        getLogger().info("BloodlineCrates enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (this.timedKeyManager != null) {
            this.timedKeyManager.shutdown();
        }
        if (this.leaderboardManager != null) {
            this.leaderboardManager.shutdown();
        }
        if (this.syncManager != null) {
            this.syncManager.shutdown();
        }
        if (this.auditLogger != null) {
            this.auditLogger.shutdown();
        }
        if (this.placementManager != null) {
            this.placementManager.shutdown();
        }
        if (this.cooldownManager != null) {
            this.cooldownManager.save();
        }
        if (this.broadcastManager != null) {
            this.broadcastManager.shutdown();
        }
        if (this.limitManager != null) {
            this.limitManager.shutdown();
        }
        if (this.discordBotManager != null) {
            this.discordBotManager.shutdown();
        }

        getLogger().info("BloodlineCrates disabled.");
    }

    private SyncManager createSyncManager() {
        String provider = getConfig().getString("sync.provider", "NONE").toUpperCase();
        switch (provider) {
            case "REDIS":
                if (isClassAvailable("redis.clients.jedis.Jedis")) {
                    return new RedisSyncManager(this);
                }
                getLogger().warning("Redis sync selected but Jedis is not available. Falling back to NONE.");
                return null;
            case "MYSQL":
                if (isClassAvailable("com.zaxxer.hikari.HikariDataSource")) {
                    return new MySQLSyncManager(this);
                }
                getLogger().warning("MySQL sync selected but HikariCP is not available. Falling back to NONE.");
                return null;
            default:
                return null;
        }
    }

    private boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private java.util.UUID extractSubject(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        String[] parts = payload.split("\\|", 2);
        if (parts.length == 0) {
            return null;
        }
        try {
            return java.util.UUID.fromString(parts[0]);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
