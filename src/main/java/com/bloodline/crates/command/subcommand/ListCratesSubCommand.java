package com.bloodline.crates.command.subcommand;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.placement.PlacedCrate;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ListCratesSubCommand implements SubCommand {
    private final BloodlineCrates plugin;

    public ListCratesSubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "listcrates";
    }

    @Override
    public String getDescription() {
        return "List all placed crates";
    }

    @Override
    public String getUsage() {
        return "[world]";
    }

    @Override
    public String getPermission() {
        return "bloodcrates.admin.listcrates";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String worldFilter = args.length >= 1 ? args[0].toLowerCase(Locale.ROOT) : null;
        List<PlacedCrate> placements = plugin.getPlacementManager().getAllPlacements().stream()
            .filter(placement -> worldFilter == null || placement.getLocation().getWorld().getName().equalsIgnoreCase(worldFilter))
            .sorted(java.util.Comparator.comparing(PlacedCrate::getCrateId))
            .collect(Collectors.toList());

        if (placements.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§7No placed crates found.");
            return;
        }

        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§6Placed crates:");
        for (PlacedCrate placement : placements) {
            var location = placement.getLocation();
            sender.sendMessage("§e- §7" + placement.getId() + " §8| §f" + placement.getCrateId()
                + " §8| §f" + location.getWorld().getName() + " "
                + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getServer().getWorlds().stream().map(world -> world.getName()).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
