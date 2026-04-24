package com.bloodline.crates.command.subcommand;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.loadout.LoadoutManager;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LoadoutSubCommand implements SubCommand {
    private final BloodlineCrates plugin;

    public LoadoutSubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "loadout";
    }

    @Override
    public String getDescription() {
        return "Save, load, list, delete, or rollback crate loadouts";
    }

    @Override
    public String getUsage() {
        return "save <name> | load <name> [--merge|--overwrite] | list | delete <name> | rollback";
    }

    @Override
    public String getPermission() {
        return "bloodcrates.admin.loadout";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /bc loadout " + getUsage());
            return;
        }

        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "save" -> save(sender, args);
                case "load" -> load(sender, args);
                case "list" -> list(sender);
                case "delete" -> delete(sender, args);
                case "rollback" -> rollback(sender);
                default -> sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUnknown loadout action.");
            }
        } catch (Exception exception) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cLoadout action failed: " + exception.getMessage());
            plugin.getLogger().warning("Loadout command failed: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("save", "load", "list", "delete", "rollback");
        }
        if (args.length == 2 && ("load".equalsIgnoreCase(args[0]) || "delete".equalsIgnoreCase(args[0]))) {
            return plugin.getLoadoutManager().listLoadouts().stream()
                .map(name -> name.replace(".json", "").replace(".yml", ""))
                .distinct()
                .toList();
        }
        if (args.length == 3 && "load".equalsIgnoreCase(args[0])) {
            return List.of("--merge", "--overwrite");
        }
        return new ArrayList<>();
    }

    private void save(CommandSender sender, String[] args) throws Exception {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /bc loadout save <name>");
            return;
        }
        var file = plugin.getLoadoutManager().saveLoadout(args[1]);
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aSaved loadout as §e" + file.getName() + "§a.");
    }

    private void load(CommandSender sender, String[] args) throws Exception {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /bc loadout load <name> [--merge|--overwrite]");
            return;
        }
        LoadoutManager.LoadMode mode = args.length >= 3 && "--overwrite".equalsIgnoreCase(args[2])
            ? LoadoutManager.LoadMode.OVERWRITE
            : LoadoutManager.LoadMode.MERGE;
        plugin.getLoadoutManager().loadLoadout(args[1], mode);
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aLoaded loadout §e" + args[1] + " §ain §e" + mode.name().toLowerCase(Locale.ROOT) + "§a mode.");
    }

    private void list(CommandSender sender) {
        List<String> loadouts = plugin.getLoadoutManager().listLoadouts();
        if (loadouts.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§7No saved loadouts found.");
            return;
        }
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§6Saved loadouts:");
        for (String loadout : loadouts) {
            sender.sendMessage("§e- §7" + loadout);
        }
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /bc loadout delete <name>");
            return;
        }
        boolean deleted = plugin.getLoadoutManager().deleteLoadout(args[1]);
        sender.sendMessage(plugin.getConfigManager().getPrefix() + (deleted ? "§aDeleted loadout §e" + args[1] + "§a." : "§cLoadout not found: " + args[1]));
    }

    private void rollback(CommandSender sender) throws Exception {
        plugin.getLoadoutManager().rollbackLatest();
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aRolled back to the latest backup.");
    }
}
