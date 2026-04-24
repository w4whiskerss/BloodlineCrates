package com.bloodline.crates.discord.command.impl;

import com.bloodline.crates.audit.AuditEntry;
import com.bloodline.crates.discord.DiscordCommandContext;
import com.bloodline.crates.discord.DiscordPermissionTier;
import com.bloodline.crates.discord.command.DiscordCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bukkit.configuration.InvalidConfigurationException;

import java.util.List;

public class ReloadCommand implements DiscordCommand {
    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Reload BloodlineCrates";
    }

    @Override
    public List<net.dv8tion.jda.api.interactions.commands.build.OptionData> getOptions() {
        return List.of();
    }

    @Override
    public DiscordPermissionTier getRequiredTier() {
        return DiscordPermissionTier.ADMIN;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, DiscordCommandContext context) {
        var buttons = context.getRouter().createConfirmationButtons(
            event.getUser().getId(),
            buttonEvent -> context.getPlugin().getServer().getScheduler().runTask(context.getPlugin(), () -> {
                try {
                    context.getPlugin().reloadConfig();
                    context.getPlugin().getConfigManager().reload();
                    context.getPlugin().getTimedKeyManager().reload();
                    context.getPlugin().getBundleManager().reload();
                    context.getPlugin().getWorldBlacklist().reload();
                    context.getPlugin().getBroadcastManager().reload();
                    context.getPlugin().getDiscordBotManager().registerSlashCommands();
                    context.getPlugin().getAuditLogger().log(new AuditEntry(
                        null,
                        event.getUser().getName(),
                        AuditEntry.AuditEventType.CONFIG_CHANGED,
                        "config.yml",
                        "discord reload command",
                        System.currentTimeMillis()
                    ));
                    buttonEvent.editMessageEmbeds(context.getEmbeds().success("Reload Complete", "BloodlineCrates was reloaded."))
                        .setComponents()
                        .queue();
                } catch (InvalidConfigurationException exception) {
                    buttonEvent.editMessageEmbeds(context.getEmbeds().error("Reload failed: " + exception.getMessage()))
                        .setComponents()
                        .queue();
                }
            }),
            buttonEvent -> buttonEvent.editMessageEmbeds(context.getEmbeds().info("Reload Cancelled", "No changes were reloaded."))
                .setComponents()
                .queue()
        );
        event.replyEmbeds(context.getEmbeds().info("Confirm Reload", "Reload BloodlineCrates config and managers?"))
            .addActionRow(buttons.get(0), buttons.get(1))
            .setEphemeral(true)
            .queue();
    }
}
