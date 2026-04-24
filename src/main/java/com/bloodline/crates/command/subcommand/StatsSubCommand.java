package com.bloodline.crates.command.subcommand;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.stats.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StatsSubCommand implements SubCommand {
    private final BloodlineCrates plugin;

    public StatsSubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public String getDescription() {
        return "View crate open stats in chat or GUI";
    }

    @Override
    public String getUsage() {
        return "[player] | gui [player]";
    }

    @Override
    public String getPermission() {
        return "bloodcrates.use";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        boolean gui = args.length > 0 && "gui".equalsIgnoreCase(args[0]);
        int playerArgIndex = gui ? 1 : 0;

        OfflinePlayer target = resolveTarget(sender, args, playerArgIndex);
        if (target == null) {
            return;
        }

        if (gui) {
            if (!(sender instanceof Player viewer)) {
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cOnly players can open the stats GUI.");
                return;
            }
            plugin.getStatsGUI().open(viewer, target);
            return;
        }

        sendChatStats(sender, target);
    }

    private void sendChatStats(CommandSender sender, OfflinePlayer target) {
        PlayerStats stats = plugin.getStatsManager().getStats(target.getUniqueId());
        String playerName = resolveDisplayName(target, stats);

        sender.sendMessage("§8§m---§r §6BloodlineCrates Stats: " + playerName + " §8§m---");
        sender.sendMessage("§7Total Opens (All Time): §f" + stats.getTotalOpensAllTime());
        sender.sendMessage("§7First Open: §f" + plugin.getStatsManager().formatDate(stats.getFirstOpenTimestamp()));
        sender.sendMessage("");

        List<String> crateIds = new ArrayList<>(stats.getOpensPerCrate().keySet());
        crateIds.removeIf(crateId -> stats.getOpensPerCrate().getOrDefault(crateId, 0) <= 0);
        crateIds.sort(Comparator.naturalOrder());

        if (crateIds.isEmpty()) {
            sender.sendMessage("§7No crate stats recorded yet.");
        } else {
            for (String crateId : crateIds) {
                Optional<Crate> crateOpt = plugin.getConfigManager().getCrate(crateId);
                String crateName = crateOpt.map(Crate::getDisplayName).orElse(crateId);
                String rarestReward = stats.getRarestRewardPerCrate().getOrDefault(crateId, "None");
                String rarestChance = plugin.getStatsManager().formatChance(stats.getRarestChancePerCrate().get(crateId));
                String lastOpened = plugin.getStatsManager().formatRelative(stats.getLastOpenedPerCrate().getOrDefault(crateId, 0L));
                sender.sendMessage("§e[" + stripColour(crateName) + "] §7Opens: §f" + stats.getOpensPerCrate().getOrDefault(crateId, 0)
                    + " §8| §7Rarest Win: §f" + stripColour(rarestReward) + " §7(" + rarestChance + "§7)"
                    + " §8| §7Last Opened: §f" + lastOpened);
            }
        }

        sender.sendMessage("");
        sender.sendMessage("§7Use §e/bloodcrates stats gui " + playerName + " §7to open the GUI view.");
    }

    private OfflinePlayer resolveTarget(CommandSender sender, String[] args, int playerArgIndex) {
        if (args.length <= playerArgIndex) {
            if (sender instanceof Player player) {
                return player;
            }
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /bc stats " + getUsage());
            return null;
        }

        OfflinePlayer target = findPlayer(args[playerArgIndex]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cPlayer not found: " + args[playerArgIndex]);
            return null;
        }

        if (sender instanceof Player player
            && !player.getUniqueId().equals(target.getUniqueId())
            && !player.hasPermission("bloodcrates.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cYou need bloodcrates.admin to view other players' stats.");
            return null;
        }

        return target;
    }

    private OfflinePlayer findPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }

        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(name)) {
                return offlinePlayer;
            }
        }
        return null;
    }

    private String resolveDisplayName(OfflinePlayer target, PlayerStats stats) {
        if (target.getName() != null && !target.getName().isBlank()) {
            return target.getName();
        }
        if (stats.getPlayerName() != null && !stats.getPlayerName().isBlank()) {
            return stats.getPlayerName();
        }
        return target.getUniqueId().toString();
    }

    private String stripColour(String value) {
        return value == null ? "" : (ChatColor.stripColor(value) == null ? value : ChatColor.stripColor(value));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("gui");
            suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            return suggestions;
        }
        if (args.length == 2 && "gui".equalsIgnoreCase(args[0])) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
