package com.bloodlinecrates.manager;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;

public final class AuditLogManager {
    private final JavaPlugin plugin;

    public AuditLogManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void log(CommandSender sender, String action) {
        File file = new File(plugin.getDataFolder(), "audit.log");
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(Instant.now() + "," + sender.getName() + "," + action + System.lineSeparator());
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to write audit log: " + exception.getMessage());
        }
    }
}
