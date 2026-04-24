package com.bloodline.crates.discord.command.impl;

import com.bloodline.crates.audit.AuditEntry;
import com.bloodline.crates.discord.DiscordCommandContext;
import com.bloodline.crates.discord.DiscordPermissionTier;
import com.bloodline.crates.discord.command.DiscordCommand;
import com.bloodline.crates.model.Crate;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Optional;

public class GiveVirtualCrateCommand implements DiscordCommand {
    @Override
    public String getName() {
        return "givevirtualcrate";
    }

    @Override
    public String getDescription() {
        return "Add virtual crates to a player";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.STRING, "crate", "Crate id", true),
            new OptionData(OptionType.STRING, "player", "Player name", true),
            new OptionData(OptionType.INTEGER, "amount", "Amount", true)
        );
    }

    @Override
    public DiscordPermissionTier getRequiredTier() {
        return DiscordPermissionTier.ADMIN;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, DiscordCommandContext context) {
        DiscordCommandSupport.deferEphemeral(event);
        String crateId = DiscordCommandSupport.stringOption(event, "crate");
        String playerName = DiscordCommandSupport.stringOption(event, "player");
        int amount = Math.max(1, DiscordCommandSupport.intOption(event, "amount", 1));
        Bukkit.getScheduler().runTask(context.getPlugin(), () -> {
            Optional<Crate> crateOpt = context.getPlugin().getConfigManager().getCrate(crateId);
            Optional<OfflinePlayer> targetOpt = DiscordCommandSupport.findOfflinePlayer(context.getPlugin(), playerName);
            if (crateOpt.isEmpty() || targetOpt.isEmpty()) {
                event.getHook().editOriginalEmbeds(context.getEmbeds().error("Crate or player not found.")).queue();
                return;
            }
            context.getPlugin().getVirtualInventoryManager().addVirtualCrate(targetOpt.get().getUniqueId(), crateOpt.get().getId(), amount);
            event.getHook().editOriginalEmbeds(context.getEmbeds().success("Give Virtual Crate", "Added " + amount + " virtual crate(s) to " + playerName + ".")).queue();
            context.getPlugin().getAuditLogger().log(new AuditEntry(null, event.getUser().getName(), AuditEntry.AuditEventType.EDITOR_ACTION, playerName, "discord givevirtualcrate " + crateOpt.get().getId() + " x" + amount, System.currentTimeMillis()));
        });
    }
}
