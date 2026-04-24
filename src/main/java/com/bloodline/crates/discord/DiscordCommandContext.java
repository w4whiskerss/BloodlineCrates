package com.bloodline.crates.discord;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.discord.command.DiscordCommandRouter;

public class DiscordCommandContext {
    private final BloodlineCrates plugin;
    private final DiscordPermissionManager permissionManager;
    private final DiscordRateLimiter rateLimiter;
    private DiscordCommandRouter router;
    private final DiscordEmbedBuilder embeds;
    private final DiscordBotManager botManager;

    public DiscordCommandContext(
        BloodlineCrates plugin,
        DiscordPermissionManager permissionManager,
        DiscordRateLimiter rateLimiter,
        DiscordCommandRouter router,
        DiscordEmbedBuilder embeds,
        DiscordBotManager botManager
    ) {
        this.plugin = plugin;
        this.permissionManager = permissionManager;
        this.rateLimiter = rateLimiter;
        this.router = router;
        this.embeds = embeds;
        this.botManager = botManager;
    }

    public BloodlineCrates getPlugin() {
        return plugin;
    }

    public DiscordPermissionManager getPermissionManager() {
        return permissionManager;
    }

    public DiscordRateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public DiscordCommandRouter getRouter() {
        return router;
    }

    public void setRouter(DiscordCommandRouter router) {
        this.router = router;
    }

    public DiscordEmbedBuilder getEmbeds() {
        return embeds;
    }

    public DiscordBotManager getBotManager() {
        return botManager;
    }
}
