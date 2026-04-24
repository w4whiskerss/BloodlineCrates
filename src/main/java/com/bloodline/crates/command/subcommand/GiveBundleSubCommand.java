package com.bloodline.crates.command.subcommand;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.bundle.CrateBundle;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GiveBundleSubCommand implements SubCommand {
    private final BloodlineCrates plugin;

    public GiveBundleSubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "givebundle";
    }

    @Override
    public String getDescription() {
        return "Give a configured crate bundle to a player";
    }

    @Override
    public String getUsage() {
        return "<bundle_id> <player>";
    }

    @Override
    public String getPermission() {
        return "bloodcrates.admin.givebundle";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("gb");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /bc givebundle <bundle_id> <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cPlayer not found: " + args[1]);
            return;
        }

        if (!plugin.getBundleManager().giveBundle(target, args[0])) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cBundle not found: " + args[0]);
            return;
        }

        CrateBundle bundle = plugin.getBundleManager().getBundle(args[0]).orElseThrow();
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aGave bundle " + bundle.getDisplayName() + " §ato " + target.getName());
        target.sendMessage(plugin.getConfigManager().getPrefix() + "§aYou received bundle " + bundle.getDisplayName() + "§a!");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getBundleManager().getBundles().stream()
                .map(CrateBundle::getId)
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
