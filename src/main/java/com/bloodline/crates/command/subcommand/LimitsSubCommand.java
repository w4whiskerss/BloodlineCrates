package com.bloodline.crates.command.subcommand;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.limit.LimitType;
import com.bloodline.crates.model.Crate;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class LimitsSubCommand implements SubCommand {
    private final BloodlineCrates plugin;

    public LimitsSubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "limits";
    }

    @Override
    public String getDescription() {
        return "Manage crate open limits";
    }

    @Override
    public String getUsage() {
        return "check <crate_id> | reset <crate_id> [global|player] [player_name] | setmax <crate_id> <global|player> <amount>";
    }

    @Override
    public String getPermission() {
        return "bloodcrates.admin.limits";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /bc limits " + getUsage());
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        Optional<Crate> crateOpt = plugin.getConfigManager().getCrate(args[1]);
        if (crateOpt.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cCrate not found: " + args[1]);
            return;
        }

        Crate crate = crateOpt.get();
        switch (action) {
            case "check" -> handleCheck(sender, crate);
            case "reset" -> handleReset(sender, crate, args);
            case "setmax" -> handleSetMax(sender, crate, args);
            default -> sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUnknown limits action.");
        }
    }

    private void handleCheck(CommandSender sender, Crate crate) {
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§eLimit info for §f" + crate.getDisplayName());
        sender.sendMessage("§7Global opens: §f" + plugin.getLimitManager().getGlobalOpens(crate.getId())
            + "§7 / §f" + (crate.isGlobalLimitEnabled() ? crate.getGlobalLimitMax() : "disabled"));
        if (sender instanceof Player player) {
            sender.sendMessage("§7Your opens: §f" + plugin.getLimitManager().getPlayerOpens(player.getUniqueId(), crate.getId())
                + "§7 / §f" + (crate.isPerPlayerLimitEnabled() ? crate.getPerPlayerLimitMax() : "disabled"));
        }
    }

    private void handleReset(CommandSender sender, Crate crate, String[] args) {
        String scope = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "global";
        if ("player".equals(scope)) {
            if (args.length >= 4) {
                Player target = Bukkit.getPlayer(args[3]);
                if (target == null) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cPlayer not found: " + args[3]);
                    return;
                }
                plugin.getLimitManager().resetPlayerLimit(crate.getId(), target.getUniqueId());
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aReset player limit for " + target.getName() + " on " + crate.getDisplayName());
                return;
            }
            plugin.getLimitManager().resetAllPlayerLimits(crate.getId());
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aReset all player limits for " + crate.getDisplayName());
            return;
        }

        plugin.getLimitManager().resetLimit(crate.getId(), LimitType.GLOBAL);
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aReset global limit for " + crate.getDisplayName());
    }

    private void handleSetMax(CommandSender sender, Crate crate, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /bc limits setmax <crate_id> <global|player> <amount>");
            return;
        }

        int amount;
        try {
            amount = Math.max(0, Integer.parseInt(args[3]));
        } catch (NumberFormatException exception) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cAmount must be a number.");
            return;
        }

        String scope = args[2].toLowerCase(Locale.ROOT);
        if ("global".equals(scope)) {
            crate.setGlobalLimitEnabled(amount > 0);
            crate.setGlobalLimitMax(amount);
            plugin.getConfigManager().saveCrate(crate);
            plugin.getPlacementManager().refreshPlacements();
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aSet global limit for " + crate.getDisplayName() + " to §e" + amount);
            return;
        }

        if ("player".equals(scope)) {
            crate.setPerPlayerLimitEnabled(amount > 0);
            crate.setPerPlayerLimitMax(amount);
            plugin.getConfigManager().saveCrate(crate);
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aSet per-player limit for " + crate.getDisplayName() + " to §e" + amount);
            return;
        }

        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cScope must be global or player.");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("check", "reset", "setmax");
        }
        if (args.length == 2) {
            return plugin.getConfigManager().getAllCrates().stream().map(Crate::getId).collect(Collectors.toList());
        }
        if (args.length == 3 && ("reset".equalsIgnoreCase(args[0]) || "setmax".equalsIgnoreCase(args[0]))) {
            return List.of("global", "player");
        }
        if (args.length == 4 && "reset".equalsIgnoreCase(args[0]) && "player".equalsIgnoreCase(args[2])) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
