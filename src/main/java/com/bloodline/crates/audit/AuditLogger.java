package com.bloodline.crates.audit;

import com.bloodline.crates.BloodlineCrates;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AuditLogger {
    private final BloodlineCrates plugin;
    private final BlockingQueue<AuditEntry> queue = new LinkedBlockingQueue<>();
    private final Thread workerThread;
    private volatile boolean running = true;
    private final File auditFolder;

    public AuditLogger(BloodlineCrates plugin) {
        this.plugin = plugin;
        this.auditFolder = new File(plugin.getDataFolder(), "audit");
        if (!auditFolder.exists()) {
            auditFolder.mkdirs();
        }

        this.workerThread = new Thread(this::drainQueue, "BloodlineCrates-AuditLogger");
        this.workerThread.setDaemon(true);
        this.workerThread.start();
    }

    public void log(AuditEntry entry) {
        queue.offer(entry);
    }

    public List<String> getEntriesForPlayer(String playerName, int page, int pageSize) {
        List<String> lines = new ArrayList<>();
        File file = todayAuditFile();
        if (!file.exists()) {
            return lines;
        }

        try {
            for (String line : Files.readAllLines(file.toPath())) {
                if (line.toLowerCase().contains("\"actorName\":\"" + playerName.toLowerCase() + "\"")
                    || line.toLowerCase().contains("\"targetId\":\"" + playerName.toLowerCase() + "\"")) {
                    lines.add(line);
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to read audit log: " + exception.getMessage());
        }

        int fromIndex = Math.max(0, lines.size() - (page * pageSize));
        int toIndex = Math.max(0, lines.size() - ((page - 1) * pageSize));
        if (fromIndex >= toIndex) {
            return new ArrayList<>();
        }
        return lines.subList(fromIndex, toIndex);
    }

    public void shutdown() {
        running = false;
        workerThread.interrupt();
    }

    private void drainQueue() {
        while (running || !queue.isEmpty()) {
            try {
                AuditEntry entry = queue.poll(1, TimeUnit.SECONDS);
                if (entry != null) {
                    writeEntry(entry);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void writeEntry(AuditEntry entry) {
        File file = auditFileFor(entry.getTimestamp());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write(toJson(entry));
            writer.newLine();
            if (plugin.getDiscordBotManager() != null) {
                plugin.getDiscordBotManager().getAlertManager().mirrorAuditEntry(entry);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to write audit entry: " + exception.getMessage());
        }
    }

    private File todayAuditFile() {
        return auditFileFor(System.currentTimeMillis());
    }

    private File auditFileFor(long timestamp) {
        LocalDate date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
        return new File(auditFolder, "audit-" + date.format(DateTimeFormatter.ISO_DATE) + ".log");
    }

    private String toJson(AuditEntry entry) {
        return "{"
            + "\"actorUUID\":\"" + escape(String.valueOf(entry.getActorUUID())) + "\","
            + "\"actorName\":\"" + escape(entry.getActorName()) + "\","
            + "\"eventType\":\"" + entry.getEventType().name() + "\","
            + "\"targetId\":\"" + escape(entry.getTargetId()) + "\","
            + "\"detail\":\"" + escape(entry.getDetail()) + "\","
            + "\"timestamp\":" + entry.getTimestamp()
            + "}";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
