package com.bloodline.crates.command.subcommand;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class InventorySubCommand implements SubCommand {
    private final BloodlineCrates plugin;

    public InventorySubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "inventory";
    }

    @Override
    public String getDescription() {
        return "Open a virtual crate inventory";
    }

    @Override
    public String getUsage() {
        return "[player]";
    }

    @Override
    public String getPermission() {
        return "bloodcrates.use";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("inv");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cOnly players can use this command.");
            return;
        }

        if (!plugin.getConfig().getBoolean("virtual-inventory.enabled", true)) {
            viewer.sendMessage(plugin.getConfigManager().getPrefix() + "§cVirtual crate inventory is disabled.");
            return;
        }

        if (args.length == 0) {
            plugin.getVirtualInventoryManager().openGUI(viewer);
            return;
        }

        if (!viewer.hasPermission("bloodcrates.admin")) {
            viewer.sendMessage(plugin.getConfigManager().getPrefix() + "§cYou do not have permission to view other players' inventories.");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            viewer.sendMessage(plugin.getConfigManager().getPrefix() + "§cPlayer not found: " + args[0]);
            return;
        }

        plugin.getVirtualInventoryManager().openGUI(viewer, target, true);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("bloodcrates.admin")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
