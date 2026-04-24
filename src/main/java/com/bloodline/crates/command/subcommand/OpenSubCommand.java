package com.bloodline.crates.command.subcommand;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.model.Crate;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OpenSubCommand implements SubCommand {
    private final BloodlineCrates plugin;
    
    public OpenSubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "open";
    }
    
    @Override
    public String getDescription() {
        return "Open a crate you're standing on";
    }
    
    @Override
    public String getUsage() {
        return "<crate_id>";
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
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cOnly players can use this command.");
            return;
        }
        
        if (args.length < 1) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /bc open <crate_id>");
            return;
        }
        
        Player player = (Player) sender;
        String crateId = args[0];
        
        Optional<Crate> crateOpt = plugin.getConfigManager().getCrate(crateId);
        if (!crateOpt.isPresent()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cCrate not found: " + crateId);
            return;
        }
        
        Crate crate = crateOpt.get();
        
        Optional<String> locationCrateId = plugin.getPlacementManager().getCrateAt(player.getLocation().getBlock().getLocation())
            .map(placedCrate -> placedCrate.getCrateId());
        if (!locationCrateId.isPresent() || !locationCrateId.get().equals(crateId)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cYou must be standing on a " + crate.getDisplayName() + " §cto open it!");
            return;
        }
        
        plugin.getCrateManager().handleCrateInteraction(player, player.getLocation().getBlock().getLocation());
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getConfigManager().getAllCrates().stream()
                .map(Crate::getId)
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
