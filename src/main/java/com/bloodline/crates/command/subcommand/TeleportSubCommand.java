package com.bloodline.crates.command.subcommand;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TeleportSubCommand implements SubCommand {
    private final BloodlineCrates plugin;

    public TeleportSubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "teleport";
    }

    @Override
    public String getDescription() {
        return "Teleport to a placed crate by placement UUID";
    }

    @Override
    public String getUsage() {
        return "<placement_id>";
    }

    @Override
    public String getPermission() {
        return "bloodcrates.admin.teleportcrate";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("tpcrate");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cOnly players can use this command.");
            return;
        }
        if (args.length < 1) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /bc teleport <placement_id>");
            return;
        }

        UUID targetId;
        try {
            targetId = UUID.fromString(args[0]);
        } catch (IllegalArgumentException exception) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cInvalid placement UUID.");
            return;
        }

        plugin.getPlacementManager().getAllPlacements().stream()
            .filter(placement -> placement.getId().equals(targetId))
            .findFirst()
            .ifPresentOrElse(placement -> {
                player.teleport(placement.getLocation().clone().add(0.5D, 1.0D, 0.5D));
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§aTeleported to placed crate §e" + placement.getCrateId() + "§a.");
            }, () -> player.sendMessage(plugin.getConfigManager().getPrefix() + "§cPlaced crate not found: " + args[0]));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getPlacementManager().getAllPlacements().stream()
                .map(placement -> placement.getId().toString())
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
