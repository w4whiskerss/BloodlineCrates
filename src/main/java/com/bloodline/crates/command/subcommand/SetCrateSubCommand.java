package com.bloodline.crates.command.subcommand;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.model.Crate;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SetCrateSubCommand implements SubCommand {
    private final BloodlineCrates plugin;

    public SetCrateSubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "setcrate";
    }

    @Override
    public String getDescription() {
        return "Place a crate at the block you're looking at";
    }

    @Override
    public String getUsage() {
        return "<crate_id>";
    }

    @Override
    public String getPermission() {
        return "bloodcrates.admin.setcrate";
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
        if (args.length < 1) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /bc setcrate <crate_id>");
            return;
        }

        Optional<Crate> crate = plugin.getConfigManager().getCrate(args[0]);
        if (crate.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cCrate not found: " + args[0]);
            return;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cLook at a block within 5 blocks.");
            return;
        }

        try {
            plugin.getPlacementManager().placeCrate(player, crate.get().getId(), targetBlock.getLocation());
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§aPlaced crate §e" + crate.get().getDisplayName() + "§a.");
        } catch (Exception exception) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c" + exception.getMessage());
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getConfigManager().getAllCrates().stream().map(Crate::getId).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
