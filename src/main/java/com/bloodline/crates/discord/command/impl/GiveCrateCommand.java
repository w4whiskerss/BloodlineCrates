package com.bloodline.crates.discord.command.impl;

import com.bloodline.crates.audit.AuditEntry;
import com.bloodline.crates.discord.DiscordCommandContext;
import com.bloodline.crates.discord.DiscordPermissionTier;
import com.bloodline.crates.discord.command.DiscordCommand;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.Reward;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public class GiveCrateCommand implements DiscordCommand {
    @Override
    public String getName() {
        return "givecrate";
    }

    @Override
    public String getDescription() {
        return "Give a crate item to a player";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.STRING, "crate", "Crate id", true),
            new OptionData(OptionType.STRING, "player", "Player name", true)
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
        Bukkit.getScheduler().runTask(context.getPlugin(), () -> {
            Optional<Crate> crateOpt = context.getPlugin().getConfigManager().getCrate(crateId);
            Optional<OfflinePlayer> targetOpt = DiscordCommandSupport.findOfflinePlayer(context.getPlugin(), playerName);
            if (crateOpt.isEmpty() || targetOpt.isEmpty()) {
                event.getHook().editOriginalEmbeds(context.getEmbeds().error("Crate or player not found.")).queue();
                return;
            }
            Crate crate = crateOpt.get();
            OfflinePlayer target = targetOpt.get();
            Player online = target.getPlayer();
            if (online != null && online.isOnline()) {
                if (crate.isVirtualOnly()) {
                    context.getPlugin().getVirtualInventoryManager().addVirtualCrate(online.getUniqueId(), crate.getId(), 1);
                } else {
                    online.getInventory().addItem(context.getPlugin().getCrateManager().createCrateItem(crate, 1));
                }
                event.getHook().editOriginalEmbeds(context.getEmbeds().success("Give Crate", "Granted " + crate.getDisplayName() + " to " + online.getName() + ".")).queue();
            } else {
                if (crate.isVirtualOnly()) {
                    context.getPlugin().getVirtualInventoryManager().addVirtualCrate(target.getUniqueId(), crate.getId(), 1);
                    event.getHook().editOriginalEmbeds(context.getEmbeds().info("Give Crate", "Player is offline - virtual crate was added directly.")).queue();
                } else {
                    context.getPlugin().getPlayerDataManager().addPendingReward(target.getUniqueId(),
                        new Reward(context.getPlugin().getCrateManager().createCrateItem(crate, 1), 100.0D, ""));
                    event.getHook().editOriginalEmbeds(context.getEmbeds().info("Give Crate", "Player is offline - crate queued for delivery via claims.")).queue();
                }
            }
            context.getPlugin().getAuditLogger().log(new AuditEntry(null, event.getUser().getName(), AuditEntry.AuditEventType.EDITOR_ACTION, playerName, "discord givecrate " + crate.getId(), System.currentTimeMillis()));
        });
    }
}
