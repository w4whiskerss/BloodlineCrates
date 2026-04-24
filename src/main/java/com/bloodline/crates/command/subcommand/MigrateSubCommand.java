package com.bloodline.crates.command.subcommand;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.migration.MigrationResult;
import com.bloodline.crates.migration.MigrationSource;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MigrateSubCommand implements SubCommand {
    private final BloodlineCrates plugin;

    public MigrateSubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "migrate";
    }

    @Override
    public String getDescription() {
        return "Dry-run or import crates from supported plugins";
    }

    @Override
    public String getUsage() {
        return "<crate_reloaded|epic_crates|casino_crates> [--confirm] [--overwrite]";
    }

    @Override
    public String getPermission() {
        return "bloodcrates.admin.migrate";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUsage: /bc migrate " + getUsage());
            return;
        }

        MigrationSource source;
        try {
            source = MigrationSource.valueOf(args[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUnknown migration source: " + args[0]);
            return;
        }

        boolean confirm = Arrays.asList(args).contains("--confirm");
        boolean overwrite = Arrays.asList(args).contains("--overwrite");
        MigrationResult result = confirm
            ? plugin.getMigrationManager().confirm(source, overwrite)
            : plugin.getMigrationManager().dryRun(source);

        renderSummary(sender, source, result, confirm);
        if (!confirm) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§eRun /bc migrate " + source.name().toLowerCase(Locale.ROOT) + " --confirm to write files.");
        }
    }

    private void renderSummary(CommandSender sender, MigrationSource source, MigrationResult result, boolean confirmed) {
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§6Migration " + (confirmed ? "result" : "dry-run") + " for §e" + source.name());
        sender.sendMessage("§7Crates imported: §f" + result.getCratesImported());
        sender.sendMessage("§7Crates skipped: §f" + result.getCratesSkipped());
        sender.sendMessage("§7Rewards imported: §f" + result.getRewardsImported());
        sender.sendMessage("§7Rewards skipped: §f" + result.getRewardsSkipped());
        renderPagedList(sender, "Warnings", result.getWarnings(), "§e");
        renderPagedList(sender, "Errors", result.getErrors(), "§c");
    }

    private void renderPagedList(CommandSender sender, String label, List<String> entries, String color) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        int page = 1;
        for (int index = 0; index < entries.size(); index += 20) {
            int end = Math.min(entries.size(), index + 20);
            sender.sendMessage(color + label + " §7(Page " + page + ")");
            for (String entry : new ArrayList<>(entries.subList(index, end))) {
                sender.sendMessage(color + "- " + entry);
            }
            page++;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("crate_reloaded", "epic_crates", "casino_crates");
        }
        if (args.length >= 2) {
            return List.of("--confirm", "--overwrite");
        }
        return Collections.emptyList();
    }
}
