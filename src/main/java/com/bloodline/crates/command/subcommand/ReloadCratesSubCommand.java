package com.bloodline.crates.command.subcommand;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.audit.AuditEntry;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class ReloadCratesSubCommand implements SubCommand {
    private final BloodlineCrates plugin;

    public ReloadCratesSubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "reloadcrates";
    }

    @Override
    public String getDescription() {
        return "Reload crate files, reset cooldowns, and refresh placed crates";
    }

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public String getPermission() {
        return "bloodcrates.admin.reloadcrates";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("rcrates");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        try {
            plugin.getConfigManager().reload();
            plugin.getCooldownManager().resetAllCooldowns();
            if (plugin.getPlacementManager() != null) {
                plugin.getPlacementManager().refreshPlacements();
            }
            plugin.getAuditLogger().log(new AuditEntry(
                null,
                sender.getName(),
                AuditEntry.AuditEventType.CONFIG_CHANGED,
                "crates/",
                "Reloaded crate configurations and reset cooldowns",
                System.currentTimeMillis()
            ));
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "\u00A7aCrates reloaded, cooldowns reset, and placed crates refreshed!");
        } catch (Exception exception) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "\u00A7cFailed to reload crates: " + exception.getMessage());
            plugin.getLogger().severe("Crate reload failed: " + exception.getMessage());
            exception.printStackTrace();
        }
    }
}
