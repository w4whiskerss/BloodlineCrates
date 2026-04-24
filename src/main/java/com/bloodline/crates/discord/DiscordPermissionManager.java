package com.bloodline.crates.discord;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

public class DiscordPermissionManager {
    private final DiscordConfig config;

    public DiscordPermissionManager(DiscordConfig config) {
        this.config = config;
    }

    public boolean hasPermission(Member member, DiscordPermissionTier tier) {
        if (tier == DiscordPermissionTier.ANY) {
            return true;
        }
        if (member == null) {
            return false;
        }

        Guild guild = member.getGuild();
        boolean owner = guild.getOwnerId().equals(member.getId());
        boolean admin = owner || member.getRoles().stream().anyMatch(role -> config.getAdminRoleIds().contains(role.getId()));
        if (tier == DiscordPermissionTier.ADMIN) {
            return admin;
        }
        return admin || member.getRoles().stream().anyMatch(role -> config.getModeratorRoleIds().contains(role.getId()));
    }
}
