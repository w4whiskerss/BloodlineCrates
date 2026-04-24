package com.bloodline.crates.discord.command;

import com.bloodline.crates.discord.DiscordCommandContext;
import com.bloodline.crates.discord.DiscordPermissionTier;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public interface DiscordCommand {
    String getName();

    String getDescription();

    List<OptionData> getOptions();

    DiscordPermissionTier getRequiredTier();

    default String getGroupName() {
        return null;
    }

    void execute(SlashCommandInteractionEvent event, DiscordCommandContext context);
}
