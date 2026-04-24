package com.bloodline.crates.command;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.command.subcommand.*;
import com.bloodline.crates.failsafe.ClaimsCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

public class BloodCratesCommand implements CommandExecutor, TabCompleter {
    private final BloodlineCrates plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public BloodCratesCommand(BloodlineCrates plugin) {
        this.plugin = plugin;

        registerSubCommand(new GiveCrateSubCommand(plugin));
        registerSubCommand(new GiveVirtualCrateSubCommand(plugin));
        registerSubCommand(new GiveKeySubCommand(plugin));
        registerSubCommand(new GiveVirtualKeySubCommand(plugin));
        registerSubCommand(new InventorySubCommand(plugin));
        registerSubCommand(new OpenSubCommand(plugin));
        registerSubCommand(new PreviewSubCommand(plugin));
        registerSubCommand(new ReloadSubCommand(plugin));
        registerSubCommand(new ReloadCratesSubCommand(plugin));
        registerSubCommand(new EditorSubCommand(plugin));
        registerSubCommand(new ClaimsCommand(plugin));
        registerSubCommand(new GiveBundleSubCommand(plugin));
        registerSubCommand(new AuditSubCommand(plugin));
        registerSubCommand(new LoadoutSubCommand(plugin));
        registerSubCommand(new SetCrateSubCommand(plugin));
        registerSubCommand(new RemoveCrateSubCommand(plugin));
        registerSubCommand(new ListCratesSubCommand(plugin));
        registerSubCommand(new TeleportSubCommand(plugin));
        registerSubCommand(new LimitsSubCommand(plugin));
        registerSubCommand(new MigrateSubCommand(plugin));
        registerSubCommand(new DebugSubCommand(plugin));
        registerSubCommand(new StatsSubCommand(plugin));
    }

    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
        for (String alias : subCommand.getAliases()) {
            subCommands.put(alias.toLowerCase(), subCommand);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);
        if (subCommand == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUnknown subcommand. Use /bloodcrates for help.");
            return true;
        }

        if (!sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cYou don't have permission to use this command.");
            return true;
        }

        try {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            subCommand.execute(sender, subArgs);
        } catch (Exception e) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cAn error occurred: " + e.getMessage());
            plugin.getLogger().severe("Error executing command: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            for (SubCommand subCommand : new HashSet<>(subCommands.values())) {
                if (sender.hasPermission(subCommand.getPermission())) {
                    completions.add(subCommand.getName());
                }
            }
            return completions;
        }

        if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null && sender.hasPermission(subCommand.getPermission())) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return subCommand.tabComplete(sender, subArgs);
            }
        }

        return Collections.emptyList();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§8§m                    §r §6BloodlineCrates §8§m                    ");
        sender.sendMessage("");

        for (SubCommand subCommand : new HashSet<>(subCommands.values())) {
            if (sender.hasPermission(subCommand.getPermission())) {
                sender.sendMessage("§e/bc " + subCommand.getName() + " §7" + subCommand.getUsage());
                sender.sendMessage("  §8» §7" + subCommand.getDescription());
            }
        }

        sender.sendMessage("");
        sender.sendMessage("§8§m                                                      ");
    }
}
