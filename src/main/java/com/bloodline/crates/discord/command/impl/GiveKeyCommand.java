package com.bloodline.crates.discord.command.impl;

import com.bloodline.crates.audit.AuditEntry;
import com.bloodline.crates.discord.DiscordCommandContext;
import com.bloodline.crates.discord.DiscordPermissionTier;
import com.bloodline.crates.discord.command.DiscordCommand;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.CrateKey;
import com.bloodline.crates.model.KeyMode;
import com.bloodline.crates.model.Reward;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public class GiveKeyCommand implements DiscordCommand {
    @Override
    public String getName() {
        return "givekey";
    }

    @Override
    public String getDescription() {
        return "Give keys to a player";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.STRING, "crate", "Crate id", true),
            new OptionData(OptionType.STRING, "player", "Player name", true),
            new OptionData(OptionType.INTEGER, "amount", "Key amount", true)
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
            if (crateOpt.isEmpty()) {
                event.getHook().editOriginalEmbeds(context.getEmbeds().error("Unknown crate `" + crateId + "`.")).queue();
                return;
            }
            Crate crate = crateOpt.get();
            Optional<OfflinePlayer> targetOpt = DiscordCommandSupport.findOfflinePlayer(context.getPlugin(), playerName);
            if (targetOpt.isEmpty()) {
                event.getHook().editOriginalEmbeds(context.getEmbeds().error("Player not found.")).queue();
                return;
            }

            OfflinePlayer target = targetOpt.get();
            Player online = target.getPlayer();
            if (online != null && online.isOnline()) {
                if (crate.getKeyMode() == KeyMode.VIRTUAL) {
                    context.getPlugin().getKeyManager().giveVirtualKey(online, crate.getId(), amount);
                } else {
                    context.getPlugin().getKeyManager().giveKey(online, new CrateKey(crate.getId(), crate.getKeyDisplayItem().clone()), amount);
                }
                event.getHook().editOriginalEmbeds(context.getEmbeds().success("Give Key", "Gave " + amount + " keys to " + online.getName() + ".")).queue();
            } else {
                if (crate.getKeyMode() == KeyMode.VIRTUAL) {
                    java.util.Map<String, Integer> keys = new java.util.LinkedHashMap<>(context.getPlugin().getPlayerDataManager().getVirtualKeys(target.getUniqueId()));
                    keys.put(crate.getId().toLowerCase(java.util.Locale.ROOT), keys.getOrDefault(crate.getId().toLowerCase(java.util.Locale.ROOT), 0) + amount);
                    context.getPlugin().getPlayerDataManager().setVirtualKeys(target.getUniqueId(), keys);
                    event.getHook().editOriginalEmbeds(context.getEmbeds().info("Give Key", "Player is offline - virtual keys were added directly.")).queue();
                } else {
                    Reward queued = new Reward(crate.getKeyDisplayItem().clone(), 100.0D, "");
                    queued.getItem().setAmount(amount);
                    context.getPlugin().getPlayerDataManager().addPendingReward(target.getUniqueId(), queued);
                    event.getHook().editOriginalEmbeds(context.getEmbeds().info("Give Key", "Player is offline - key item queued for delivery via claims.")).queue();
                }
            }
            context.getPlugin().getAuditLogger().log(new AuditEntry(null, event.getUser().getName(), AuditEntry.AuditEventType.KEY_GIVEN, playerName, crate.getId() + " x" + amount, System.currentTimeMillis()));
        });
    }
}
