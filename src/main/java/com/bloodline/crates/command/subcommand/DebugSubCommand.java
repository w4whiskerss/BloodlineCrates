package com.bloodline.crates.command.subcommand;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DebugSubCommand implements SubCommand {
    private final BloodlineCrates plugin;

    public DebugSubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "debug";
    }

    @Override
    public String getDescription() {
        return "Trace crate logic for all players or one target player";
    }

    @Override
    public String getUsage() {
        return "on [player] | off | status";
    }

    @Override
    public String getPermission() {
        return "bloodcrates.admin.debug";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cThis command can only be used by players.");
            return;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /bc debug " + getUsage());
            return;
        }

        UUID adminUUID = admin.getUniqueId();
        String action = args[0].toLowerCase();
        switch (action) {
            case "on" -> enable(admin, adminUUID, args);
            case "off" -> {
                plugin.getDebugManager().disableDebug(adminUUID);
                admin.sendMessage(plugin.getConfigManager().getPrefix() + "§aDebug tracing disabled.");
            }
            case "status" -> sendStatus(admin, adminUUID);
            default -> admin.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /bc debug " + getUsage());
        }
    }

    private void enable(Player admin, UUID adminUUID, String[] args) {
        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                admin.sendMessage(plugin.getConfigManager().getPrefix() + "§cPlayer not found: " + args[1]);
                return;
            }
            plugin.getDebugManager().enableDebug(adminUUID, target.getUniqueId());
            admin.sendMessage(plugin.getConfigManager().getPrefix() + "§aDebug tracing enabled for §e" + target.getName() + "§a.");
            return;
        }

        plugin.getDebugManager().enableDebug(adminUUID, null);
        admin.sendMessage(plugin.getConfigManager().getPrefix() + "§aDebug tracing enabled for all players.");
    }

    private void sendStatus(Player admin, UUID adminUUID) {
        if (!plugin.getDebugManager().isDebugging(adminUUID)) {
            admin.sendMessage(plugin.getConfigManager().getPrefix() + "§7Debug tracing is currently disabled.");
            return;
        }

        if (plugin.getDebugManager().isDebuggingAllPlayers(adminUUID)) {
            admin.sendMessage(plugin.getConfigManager().getPrefix() + "§aDebug tracing is active for §eall players§a.");
            return;
        }

        UUID targetUUID = plugin.getDebugManager().getTargetPlayer(adminUUID);
        Player target = targetUUID == null ? null : Bukkit.getPlayer(targetUUID);
        String targetName = target != null ? target.getName() : String.valueOf(targetUUID);
        admin.sendMessage(plugin.getConfigManager().getPrefix() + "§aDebug tracing is active for §e" + targetName + "§a.");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("on", "off", "status");
        }
        if (args.length == 2 && "on".equalsIgnoreCase(args[0])) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
