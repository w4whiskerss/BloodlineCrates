package com.bloodline.crates.failsafe;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.command.subcommand.SubCommand;
import com.bloodline.crates.model.Reward;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class ClaimsCommand implements SubCommand {
    private final BloodlineCrates plugin;

    public ClaimsCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "claims";
    }

    @Override
    public String getDescription() {
        return "View and redeem your unclaimed rewards";
    }

    @Override
    public String getUsage() {
        return "[redeem <number>]";
    }

    @Override
    public String getPermission() {
        return "bloodcrates.claims";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMessage("must-be-player"));
            return;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("redeem")) {
            redeemClaim(player, args[1]);
            return;
        }

        listClaims(player);
    }

    private void listClaims(Player player) {
        List<Reward> claims = plugin.getClaimsManager().getClaims(player.getUniqueId());
        if (claims.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMessage("claims-empty"));
            return;
        }

        player.sendMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMessage("claims-header"));
        for (int index = 0; index < claims.size(); index++) {
            Reward reward = claims.get(index);
            player.sendMessage("§e" + (index + 1) + ". §f" + reward.getDisplayNameOrFallback() + " §7- /bc claims redeem " + (index + 1));
        }
    }

    private void redeemClaim(Player player, String claimIndexRaw) {
        int claimIndex;
        try {
            claimIndex = Integer.parseInt(claimIndexRaw);
        } catch (NumberFormatException exception) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMessage("invalid-claim-number"));
            return;
        }

        if (!plugin.getClaimsManager().redeemClaim(player, claimIndex)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMessage("claim-redeem-failed"));
        }
    }
}
