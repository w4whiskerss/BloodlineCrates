package com.bloodline.crates.command.subcommand;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class RemoveCrateSubCommand implements SubCommand {
    private final BloodlineCrates plugin;

    public RemoveCrateSubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "removecrate";
    }

    @Override
    public String getDescription() {
        return "Remove the crate you're looking at";
    }

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public String getPermission() {
        return "bloodcrates.admin.removecrate";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cOnly players can use this command.");
            return;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cLook at a block within 5 blocks.");
            return;
        }

        if (plugin.getPlacementManager().removeCrate(targetBlock.getLocation()).isPresent()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§aRemoved placed crate.");
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cNo placed crate at that block.");
        }
    }
}
