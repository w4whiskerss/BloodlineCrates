package com.bloodline.crates.discord.command.impl;

import com.bloodline.crates.discord.DiscordCommandContext;
import com.bloodline.crates.discord.DiscordPermissionTier;
import com.bloodline.crates.discord.command.DiscordCommand;
import com.bloodline.crates.leaderboard.LeaderboardPeriod;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bukkit.Bukkit;

import java.util.List;

public class ServerInfoCommand implements DiscordCommand {
    @Override
    public String getName() {
        return "serverinfo";
    }

    @Override
    public String getDescription() {
        return "Show server crate information";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of();
    }

    @Override
    public DiscordPermissionTier getRequiredTier() {
        return DiscordPermissionTier.MODERATOR;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, DiscordCommandContext context) {
        event.replyEmbeds(context.getEmbeds().base("BloodlineCrates - Server Info", "Current server info", com.bloodline.crates.discord.DiscordEmbedBuilder.COLOUR_INFO)
            .addField("Configured Crates", String.valueOf(context.getPlugin().getConfigManager().getAllCrates().size()), true)
            .addField("Placed Crates", String.valueOf(context.getPlugin().getPlacementManager().getAllPlacements().size()), true)
            .addField("Online Players", String.valueOf(Bukkit.getOnlinePlayers().size()), true)
            .addField("Daily Opens", String.valueOf(context.getPlugin().getLeaderboardManager().getTotalOpens(LeaderboardPeriod.DAILY)), true)
            .build()).setEphemeral(true).queue();
    }
}
