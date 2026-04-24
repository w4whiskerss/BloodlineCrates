package com.bloodline.crates.discord;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.discord.command.DiscordCommandRouter;
import com.bloodline.crates.discord.listener.BotReadyListener;
import com.bloodline.crates.discord.listener.SlashCommandListener;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

@Getter
public class DiscordBotManager {
    private final BloodlineCrates plugin;
    private final DiscordConfig discordConfig;
    private final DiscordPermissionManager permissionManager;
    private final DiscordRateLimiter rateLimiter;
    private final DiscordEmbedBuilder embeds;
    private final DiscordAlertManager alertManager;
    private final DiscordCommandContext context;
    private final DiscordCommandRouter commandRouter;
    private volatile JDA jda;
    private volatile boolean connected;
    private BukkitTask statsTask;

    public DiscordBotManager(BloodlineCrates plugin) {
        this.plugin = plugin;
        this.discordConfig = new DiscordConfig(plugin);
        this.permissionManager = new DiscordPermissionManager(discordConfig);
        this.rateLimiter = new DiscordRateLimiter(discordConfig.getMaxCommandsPerMinutePerUser());
        this.embeds = new DiscordEmbedBuilder(plugin);
        this.alertManager = new DiscordAlertManager(plugin, this, embeds);
        this.context = new DiscordCommandContext(plugin, permissionManager, rateLimiter, null, embeds, this);
        this.commandRouter = new DiscordCommandRouter(plugin, context);
        this.context.setRouter(commandRouter);
    }

    public void start() {
        if (!discordConfig.isEnabled()) {
            plugin.getLogger().info("Discord integration disabled");
            return;
        }
        if (discordConfig.getBotToken() == null || discordConfig.getBotToken().isBlank()) {
            plugin.getLogger().severe("Discord bot token missing; Discord integration disabled for this session.");
            return;
        }
        if (discordConfig.getGuildId() == null || discordConfig.getGuildId().isBlank()) {
            plugin.getLogger().severe("Discord guild ID missing; Discord integration disabled for this session.");
            return;
        }
        if (discordConfig.getAdminRoleIds().isEmpty()) {
            plugin.getLogger().warning("Discord admin role list is empty; only the guild owner will pass ADMIN checks.");
        }

        try {
            plugin.getLogger().info("Discord bot connecting...");
            this.jda = JDABuilder.createLight(discordConfig.getBotToken(), GatewayIntent.GUILD_MEMBERS)
                .disableCache(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER)
                .setMemberCachePolicy(MemberCachePolicy.DEFAULT)
                .addEventListeners(new BotReadyListener(this), new SlashCommandListener(this))
                .build();
        } catch (Exception exception) {
            plugin.getLogger().severe("Failed to start Discord bot: " + exception.getMessage());
        }
    }

    public void shutdown() {
        if (statsTask != null) {
            statsTask.cancel();
        }
        if (jda != null) {
            try {
                jda.shutdown();
                jda.awaitShutdown(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        }
        connected = false;
    }

    public boolean isConnected() {
        return connected && jda != null;
    }

    public void onReady(JDA readyJda) {
        this.jda = readyJda;
        this.connected = true;
        plugin.getLogger().info("Discord bot connected as " + readyJda.getSelfUser().getAsTag());
        registerSlashCommands();
        scheduleStatsPosting();
    }

    public void registerSlashCommands() {
        Guild guild = getGuild();
        if (guild == null) {
            plugin.getLogger().severe("Discord guild not found; disabling Discord features for this session.");
            connected = false;
            return;
        }
        guild.updateCommands().addCommands(commandRouter.buildRootCommand()).queue();
    }

    public Guild getGuild() {
        return jda == null ? null : jda.getGuildById(discordConfig.getGuildId());
    }

    private void scheduleStatsPosting() {
        if (discordConfig.getStatsChannelId() == null) {
            return;
        }
        if (statsTask != null) {
            statsTask.cancel();
        }
        long interval = Math.max(20L, discordConfig.getStatsPostIntervalMinutes() * 60L * 20L);
        statsTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, alertManager::postScheduledStats, interval, interval);
    }
}
