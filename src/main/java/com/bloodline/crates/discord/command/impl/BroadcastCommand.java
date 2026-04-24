package com.bloodline.crates.discord.command.impl;

import com.bloodline.crates.audit.AuditEntry;
import com.bloodline.crates.discord.DiscordCommandContext;
import com.bloodline.crates.discord.DiscordPermissionTier;
import com.bloodline.crates.discord.command.DiscordCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.List;

public class BroadcastCommand implements DiscordCommand {
    @Override
    public String getName() {
        return "broadcast";
    }

    @Override
    public String getDescription() {
        return "Broadcast a message to the server";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(new OptionData(OptionType.STRING, "message", "Broadcast message", true));
    }

    @Override
    public DiscordPermissionTier getRequiredTier() {
        return DiscordPermissionTier.ADMIN;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, DiscordCommandContext context) {
        String message = DiscordCommandSupport.stringOption(event, "message");
        String formatted = ChatColor.translateAlternateColorCodes('&', message);
        var buttons = context.getRouter().createConfirmationButtons(
            event.getUser().getId(),
            buttonEvent -> {
                Bukkit.getScheduler().runTask(context.getPlugin(), () -> {
                    Bukkit.broadcastMessage(formatted);
                    context.getPlugin().getAuditLogger().log(new AuditEntry(
                        null,
                        event.getUser().getName(),
                        AuditEntry.AuditEventType.EDITOR_ACTION,
                        "server",
                        "discord broadcast: " + ChatColor.stripColor(formatted),
                        System.currentTimeMillis()
                    ));
                    buttonEvent.editMessageEmbeds(context.getEmbeds().success("Broadcast Sent", formatted)).setComponents().queue();
                });
            },
            buttonEvent -> buttonEvent.editMessageEmbeds(context.getEmbeds().info("Broadcast Cancelled", "No message was sent.")).setComponents().queue()
        );
        event.replyEmbeds(context.getEmbeds().info("Confirm Broadcast", formatted))
            .addActionRow(buttons.get(0), buttons.get(1))
            .setEphemeral(true)
            .queue();
    }
}
