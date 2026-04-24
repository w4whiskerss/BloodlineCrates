package com.bloodline.crates.command.subcommand;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class AuditSubCommand implements SubCommand {
    private final BloodlineCrates plugin;

    public AuditSubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "audit";
    }

    @Override
    public String getDescription() {
        return "View today's audit entries for a player";
    }

    @Override
    public String getUsage() {
        return "<player> [page]";
    }

    @Override
    public String getPermission() {
        return "bloodcrates.admin.audit";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /bc audit <player> [page]");
            return;
        }

        int page = 1;
        if (args.length >= 2) {
            try {
                page = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException exception) {
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cInvalid page number.");
                return;
            }
        }

        List<String> entries = plugin.getAuditLogger().getEntriesForPlayer(args[0], page, 10);
        if (entries.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§7No audit entries found for " + args[0] + " on page " + page + ".");
            return;
        }

        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§6Audit entries for " + args[0] + " (page " + page + "):");
        for (String entry : entries) {
            sender.sendMessage("§7" + entry);
        }
    }
}
