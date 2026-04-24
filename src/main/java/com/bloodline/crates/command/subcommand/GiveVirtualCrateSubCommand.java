package com.bloodline.crates.command.subcommand;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.model.Crate;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GiveVirtualCrateSubCommand implements SubCommand {
    private final BloodlineCrates plugin;

    public GiveVirtualCrateSubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "givevirtualcrate";
    }

    @Override
    public String getDescription() {
        return "Give virtual crates to a player";
    }

    @Override
    public String getUsage() {
        return "<crate_id> <player> [amount]";
    }

    @Override
    public String getPermission() {
        return "bloodcrates.admin.givevirtualcrate";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("gvc");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /bc givevirtualcrate <crate_id> <player> [amount]");
            return;
        }

        Optional<Crate> crateOpt = plugin.getConfigManager().getCrate(args[0]);
        if (crateOpt.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cCrate not found: " + args[0]);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cPlayer not found: " + args[1]);
            return;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException exception) {
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cAmount must be a number.");
                return;
            }
        }

        Crate crate = crateOpt.get();
        plugin.getVirtualInventoryManager().addVirtualCrate(target.getUniqueId(), crate.getId(), amount);
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aGave §e" + amount + "x §a" + crate.getDisplayName() + " §ato " + target.getName());
        target.sendMessage(plugin.getConfigManager().getPrefix() + "§aYou received §e" + amount + "x §a" + crate.getDisplayName() + " §avirtual crate(s)!");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getConfigManager().getAllCrates().stream().map(Crate::getId).collect(Collectors.toList());
        }
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
