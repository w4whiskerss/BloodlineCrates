package com.bloodline.crates.command.subcommand;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Collections;
import java.util.List;

public class EditorSubCommand implements SubCommand {
    private final BloodlineCrates plugin;
    
    public EditorSubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "editor";
    }
    
    @Override
    public String getDescription() {
        return "Open the in-game crate editor";
    }
    
    @Override
    public String getUsage() {
        return "";
    }
    
    @Override
    public String getPermission() {
        return "bloodcrates.admin.editor";
    }
    
    @Override
    public List<String> getAliases() {
        return Collections.singletonList("edit");
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cThis command can only be used by players.");
            return;
        }
        
        Player player = (Player) sender;
        plugin.getGuiManager().openEditorMain(player);
    }
}
