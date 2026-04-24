package com.bloodline.crates.discord;

import com.bloodline.crates.BloodlineCrates;

import java.util.List;

public class DiscordConfig {
    private final boolean enabled;
    private final String botToken;
    private final String guildId;
    private final String statsChannelId;
    private final String alertChannelId;
    private final String commandChannelId;
    private final String auditLogChannelId;
    private final int statsPostIntervalMinutes;
    private final int maxCommandsPerMinutePerUser;
    private final List<String> adminRoleIds;
    private final List<String> moderatorRoleIds;

    public DiscordConfig(BloodlineCrates plugin) {
        this.enabled = plugin.getConfig().getBoolean("discord.enabled", false);
        this.botToken = plugin.getConfig().getString("discord.bot-token", "");
        this.guildId = plugin.getConfig().getString("discord.guild-id", "");
        this.statsChannelId = plugin.getConfig().getString("discord.channels.stats", "");
        this.alertChannelId = plugin.getConfig().getString("discord.channels.alerts", "");
        this.commandChannelId = plugin.getConfig().getString("discord.channels.commands", "");
        this.auditLogChannelId = plugin.getConfig().getString("discord.channels.audit-log", "");
        this.statsPostIntervalMinutes = Math.max(1, plugin.getConfig().getInt("discord.stats-post-interval-minutes", 60));
        this.maxCommandsPerMinutePerUser = Math.max(1, plugin.getConfig().getInt("discord.rate-limit.max-commands-per-minute", 10));
        this.adminRoleIds = sanitize(plugin.getConfig().getStringList("discord.permissions.admin-role-ids"));
        this.moderatorRoleIds = sanitize(plugin.getConfig().getStringList("discord.permissions.moderator-role-ids"));
    }

    private List<String> sanitize(List<String> values) {
        return values == null ? List.of() : values.stream().filter(value -> value != null && !value.isBlank()).toList();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getBotToken() {
        return botToken;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getStatsChannelId() {
        return blankToNull(statsChannelId);
    }

    public String getAlertChannelId() {
        return blankToNull(alertChannelId);
    }

    public String getCommandChannelId() {
        return blankToNull(commandChannelId);
    }

    public String getAuditLogChannelId() {
        return blankToNull(auditLogChannelId);
    }

    public int getStatsPostIntervalMinutes() {
        return statsPostIntervalMinutes;
    }

    public int getMaxCommandsPerMinutePerUser() {
        return maxCommandsPerMinutePerUser;
    }

    public List<String> getAdminRoleIds() {
        return adminRoleIds;
    }

    public List<String> getModeratorRoleIds() {
        return moderatorRoleIds;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
