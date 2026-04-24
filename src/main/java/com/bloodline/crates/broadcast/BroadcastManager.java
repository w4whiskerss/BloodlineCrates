package com.bloodline.crates.broadcast;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.Reward;
import com.bloodline.crates.stats.StatsStore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class BroadcastManager {
    private final BloodlineCrates plugin;
    private final StatsStore statsStore;
    private final Map<String, Integer> serverOpenCounts = new LinkedHashMap<>();
    private final List<BroadcastTemplate> templates = new ArrayList<>();
    private final Set<Integer> milestones = new TreeSet<>();
    private final DecimalFormat chanceFormat = new DecimalFormat("0.00");
    private double rareThreshold;

    public BroadcastManager(BloodlineCrates plugin) {
        this.plugin = plugin;
        this.statsStore = new StatsStore(plugin);
        loadStats();
        reload();
    }

    public void reload() {
        this.templates.clear();
        this.milestones.clear();
        this.rareThreshold = plugin.getConfig().getDouble("broadcasts.rare-threshold", 5.0D);
        for (int milestone : plugin.getConfig().getIntegerList("broadcasts.milestones")) {
            if (milestone > 0) {
                milestones.add(milestone);
            }
        }

        List<?> rawTemplates = plugin.getConfig().getList("broadcasts.templates");
        if (rawTemplates != null) {
            for (Object rawTemplate : rawTemplates) {
                if (rawTemplate instanceof Map<?, ?> map) {
                    BroadcastTemplate template = parseTemplate(map);
                    if (template != null) {
                        templates.add(template);
                    }
                }
            }
            return;
        }

        ConfigurationSection templatesSection = plugin.getConfig().getConfigurationSection("broadcasts.templates");
        if (templatesSection == null) {
            return;
        }

        for (String key : templatesSection.getKeys(false)) {
            ConfigurationSection section = templatesSection.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            BroadcastTemplate template = parseTemplate(section.getValues(false));
            if (template != null) {
                templates.add(template);
            }
        }
    }

    public int getServerOpens(String crateId) {
        return serverOpenCounts.getOrDefault(normalize(crateId), 0);
    }

    public int incrementServerOpens(String crateId) {
        String normalized = normalize(crateId);
        int updated = getServerOpens(normalized) + 1;
        serverOpenCounts.put(normalized, updated);
        return updated;
    }

    public void checkAndBroadcast(Player player, Crate crate, Reward reward) {
        checkAndBroadcast(player, crate, reward, false, plugin.getPlayerDataManager().getCrateOpenCount(player.getUniqueId(), crate.getId()), getServerOpens(crate.getId()));
    }

    public void checkAndBroadcast(Player player, Crate crate, Reward reward, boolean pityTriggered, int playerOpens, int serverOpens) {
        if (templates.isEmpty()) {
            return;
        }

        EnumSet<BroadcastTrigger> matchedTriggers = EnumSet.noneOf(BroadcastTrigger.class);
        if (reward.getChance() <= rareThreshold) {
            matchedTriggers.add(BroadcastTrigger.RARE_REWARD);
        }
        if (pityTriggered) {
            matchedTriggers.add(BroadcastTrigger.JACKPOT);
        }
        if (serverOpens == 1) {
            matchedTriggers.add(BroadcastTrigger.FIRST_OPEN);
        }
        if (milestones.contains(playerOpens)) {
            matchedTriggers.add(BroadcastTrigger.MILESTONE);
        }
        if (reward.isBroadcast()) {
            matchedTriggers.add(BroadcastTrigger.SPECIFIC_REWARD);
        }

        if (matchedTriggers.isEmpty()) {
            return;
        }

        for (BroadcastTemplate template : templates) {
            if (template.getTrigger() == null || !matchedTriggers.contains(template.getTrigger())) {
                continue;
            }
            executeTemplate(player, crate, reward, template, playerOpens, serverOpens);
        }
    }

    public void shutdown() {
        saveStats();
    }

    public void saveStatsNow() {
        saveStats();
    }

    private void executeTemplate(Player player, Crate crate, Reward reward, BroadcastTemplate template, int playerOpens, int serverOpens) {
        List<String> chatLines = resolveChatLines(template, reward);
        for (String line : chatLines) {
            String resolved = applyPlaceholders(line, player, crate, reward, playerOpens, serverOpens);
            String colored = ChatColor.translateAlternateColorCodes('&', resolved);
            Bukkit.getOnlinePlayers().forEach(target -> target.sendMessage(colored));
        }

        String title = emptyToNull(applyPlaceholders(template.getTitleText(), player, crate, reward, playerOpens, serverOpens));
        String subtitle = emptyToNull(applyPlaceholders(template.getSubtitleText(), player, crate, reward, playerOpens, serverOpens));
        if (title != null || subtitle != null) {
            player.sendTitle(
                title == null ? "" : ChatColor.translateAlternateColorCodes('&', title),
                subtitle == null ? "" : ChatColor.translateAlternateColorCodes('&', subtitle),
                template.getTitleFadeIn(),
                template.getTitleStay(),
                template.getTitleFadeOut()
            );
        }

        if (template.getSoundOnBroadcast() != null && !template.getSoundOnBroadcast().isBlank()) {
            try {
                Sound sound = Sound.valueOf(template.getSoundOnBroadcast().toUpperCase(Locale.ROOT));
                Bukkit.getOnlinePlayers().forEach(target -> target.playSound(target.getLocation(), sound, 1.0F, 1.0F));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Invalid broadcast sound: " + template.getSoundOnBroadcast());
            }
        }

        if (template.isSendToDiscord()) {
            plugin.getDiscordWebhookManager().sendCrateOpenEvent(player, crate, reward, true);
        }
    }

    private List<String> resolveChatLines(BroadcastTemplate template, Reward reward) {
        if (template.getTrigger() == BroadcastTrigger.SPECIFIC_REWARD
            && reward.getBroadcastMessage() != null
            && !reward.getBroadcastMessage().isBlank()) {
            return List.of(reward.getBroadcastMessage());
        }
        return template.getChatLines() == null ? List.of() : template.getChatLines();
    }

    private String applyPlaceholders(String input, Player player, Crate crate, Reward reward, int playerOpens, int serverOpens) {
        if (input == null) {
            return null;
        }
        return input
            .replace("{player}", player.getDisplayName())
            .replace("{crate}", crate.getDisplayName())
            .replace("{reward}", reward.getDisplayNameOrFallback())
            .replace("{chance}", chanceFormat.format(reward.getChance()))
            .replace("{server_opens}", String.valueOf(serverOpens))
            .replace("{player_opens}", String.valueOf(playerOpens));
    }

    private BroadcastTemplate parseTemplate(Map<?, ?> raw) {
        Object rawTrigger = raw.get("trigger");
        if (rawTrigger == null) {
            return null;
        }

        BroadcastTrigger trigger;
        try {
            trigger = BroadcastTrigger.valueOf(String.valueOf(rawTrigger).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }

        BroadcastTemplate template = new BroadcastTemplate();
        template.setTrigger(trigger);
        Object rawChat = raw.get("chat");
        if (rawChat instanceof List<?> list) {
            List<String> lines = new ArrayList<>();
            for (Object line : list) {
                lines.add(String.valueOf(line));
            }
            template.setChatLines(lines);
        }
        template.setTitleText(stringValue(raw.get("title")));
        template.setSubtitleText(stringValue(raw.get("subtitle")));
        template.setTitleFadeIn(intValue(raw.get("fade-in"), 10));
        template.setTitleStay(intValue(raw.get("stay"), 60));
        template.setTitleFadeOut(intValue(raw.get("fade-out"), 20));
        template.setSendToDiscord(booleanValue(raw.get("discord")));
        template.setSoundOnBroadcast(stringValue(raw.get("sound")));
        return template;
    }

    private void loadStats() {
        serverOpenCounts.clear();
        serverOpenCounts.putAll(statsStore.loadServerOpens());
    }

    private void saveStats() {
        statsStore.saveServerOpens(serverOpenCounts);
    }

    private String normalize(String crateId) {
        return crateId == null ? "" : crateId.toLowerCase(Locale.ROOT);
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private boolean booleanValue(Object value) {
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
