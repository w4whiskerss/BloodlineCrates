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

public class GiveCrateSubCommand implements SubCommand {
    private final BloodlineCrates plugin;

    public GiveCrateSubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "givecrate";
    }

    @Override
    public String getDescription() {
        return "Give a crate block or virtual crate to a player";
    }

    @Override
    public String getUsage() {
        return "<crate_id> <player>";
    }

    @Override
    public String getPermission() {
        return "bloodcrates.admin.givecrate";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("gc");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /bc givecrate <crate_id> <player>");
            return;
        }

        String crateId = args[0];
        Optional<Crate> crateOpt = plugin.getConfigManager().getCrate(crateId);
        if (crateOpt.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cCrate not found: " + crateId);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cPlayer not found: " + args[1]);
            return;
        }

        Crate crate = crateOpt.get();
        if (crate.isVirtualOnly()) {
            plugin.getVirtualInventoryManager().addVirtualCrate(target.getUniqueId(), crate.getId(), 1);
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aAdded a virtual " + crate.getDisplayName() + " §ato " + target.getName());
            target.sendMessage(plugin.getConfigManager().getPrefix() + "§aYou received a virtual " + crate.getDisplayName() + "§a!");
            return;
        }

        target.getInventory().addItem(plugin.getCrateManager().createCrateItem(crate, 1));
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aGave " + crate.getDisplayName() + " §ato " + target.getName());
        target.sendMessage(plugin.getConfigManager().getPrefix() + "§aYou received a " + crate.getDisplayName() + "§a!");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getConfigManager().getAllCrates().stream()
                .map(Crate::getId)
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
