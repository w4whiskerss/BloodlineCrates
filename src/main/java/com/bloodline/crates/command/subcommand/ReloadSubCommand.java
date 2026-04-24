package com.bloodline.crates.command.subcommand;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.audit.AuditEntry;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;

import java.util.Collections;
import java.util.List;

public class ReloadSubCommand implements SubCommand {
    private final BloodlineCrates plugin;

    public ReloadSubCommand(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Reload all configurations";
    }

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public String getPermission() {
        return "bloodcrates.admin.reload";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("rl");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        try {
            plugin.reloadConfig();
            plugin.getConfigManager().reload();
            plugin.getTimedKeyManager().reload();
            plugin.getBundleManager().reload();
            plugin.getWorldBlacklist().reload();
            plugin.getBroadcastManager().reload();
            plugin.getAuditLogger().log(new AuditEntry(
                null,
                sender.getName(),
                AuditEntry.AuditEventType.CONFIG_CHANGED,
                "config.yml",
                "Reloaded plugin configuration",
                System.currentTimeMillis()
            ));
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aConfiguration reloaded successfully!");
        } catch (InvalidConfigurationException e) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cFailed to reload: " + e.getMessage());
            plugin.getLogger().severe("Reload failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
