package com.bloodline.crates.command.subcommand;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.model.Crate;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PreviewSubCommand implements SubCommand {
    private final BloodlineCrates plugin;
    
    public PreviewSubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "preview";
    }
    
    @Override
    public String getDescription() {
        return "Preview rewards in a crate";
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
        return Collections.singletonList("view");
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cOnly players can use this command.");
            return;
        }
        
        if (args.length < 1) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /bc preview <crate_id>");
            return;
        }
        
        Player player = (Player) sender;
        String crateId = args[0];
        
        Optional<Crate> crateOpt = plugin.getConfigManager().getCrate(crateId);
        if (!crateOpt.isPresent()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cCrate not found: " + crateId);
            return;
        }
        
        plugin.getGuiManager().openPreview(player, crateOpt.get());
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