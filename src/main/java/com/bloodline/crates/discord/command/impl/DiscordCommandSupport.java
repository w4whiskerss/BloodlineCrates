package com.bloodline.crates.discord.command.impl;

import com.bloodline.crates.BloodlineCrates;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Optional;

final class DiscordCommandSupport {
    private DiscordCommandSupport() {
    }

    static void deferEphemeral(SlashCommandInteractionEvent event) {
        if (!event.isAcknowledged()) {
            event.deferReply(true).queue();
        }
    }

    static Optional<OfflinePlayer> findOfflinePlayer(BloodlineCrates plugin, String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return Optional.of(online);
        }
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if (player.getName() != null && player.getName().equalsIgnoreCase(name)) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }

    static String stringOption(SlashCommandInteractionEvent event, String name) {
        OptionMapping option = event.getOption(name);
        return option == null ? "" : option.getAsString();
    }

    static int intOption(SlashCommandInteractionEvent event, String name, int fallback) {
        OptionMapping option = event.getOption(name);
        return option == null ? fallback : (int) option.getAsLong();
    }

    static String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
