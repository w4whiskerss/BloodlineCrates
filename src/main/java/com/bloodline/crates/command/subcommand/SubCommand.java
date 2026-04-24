package com.bloodline.crates.command.subcommand;

import org.bukkit.command.CommandSender;
import java.util.Collections;
import java.util.List;

public interface SubCommand {
    String getName();
    String getDescription();
    String getUsage();
    String getPermission();
    List<String> getAliases();
    void execute(CommandSender sender, String[] args);
    
    default List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}