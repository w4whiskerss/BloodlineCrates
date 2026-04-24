package com.bloodline.crates.discord.command.impl;

import com.bloodline.crates.discord.DiscordCommandContext;
import com.bloodline.crates.discord.DiscordPermissionTier;
import com.bloodline.crates.discord.command.DiscordCommand;
import com.bloodline.crates.leaderboard.LeaderboardEntry;
import com.bloodline.crates.leaderboard.LeaderboardPeriod;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardCommand implements DiscordCommand {
    @Override
    public String getName() {
        return "leaderboard";
    }

    @Override
    public String getDescription() {
        return "Show the crate leaderboard";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(new OptionData(OptionType.STRING, "period", "Leaderboard period", true)
            .addChoices(
                new Command.Choice("DAILY", "DAILY"),
                new Command.Choice("WEEKLY", "WEEKLY"),
                new Command.Choice("MONTHLY", "MONTHLY"),
                new Command.Choice("YEARLY", "YEARLY")
            ));
    }

    @Override
    public DiscordPermissionTier getRequiredTier() {
        return DiscordPermissionTier.MODERATOR;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, DiscordCommandContext context) {
        DiscordCommandSupport.deferEphemeral(event);
        LeaderboardPeriod period = LeaderboardPeriod.valueOf(DiscordCommandSupport.stringOption(event, "period"));
        List<LeaderboardEntry> entries = context.getPlugin().getLeaderboardManager().getLeaderboard(period, 10);
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            String medal = switch (i) {
                case 0 -> "🥇";
                case 1 -> "🥈";
                case 2 -> "🥉";
                default -> "#" + (i + 1);
            };
            lines.add(medal + " " + entries.get(i).getPlayerName() + " - " + entries.get(i).getCrateOpens());
        }
        if (lines.isEmpty()) {
            lines.add("No data.");
        }
        event.replyEmbeds(context.getEmbeds().info("Leaderboard", period.name() + "\n\n" + String.join("\n", lines))).setEphemeral(true).queue();
    }
}
