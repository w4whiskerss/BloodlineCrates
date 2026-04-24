package com.bloodline.crates.hook;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.Reward;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DiscordWebhookManager {
    private final BloodlineCrates plugin;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public DiscordWebhookManager(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    public void sendCrateOpenEvent(Player player, Crate crate, Reward reward) {
        sendCrateOpenEvent(player, crate, reward, false);
    }

    public void sendCrateOpenEvent(Player player, Crate crate, Reward reward, boolean force) {
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) {
            return;
        }

        String webhookUrl = plugin.getConfig().getString("discord.webhook-url", "");
        if (webhookUrl.isBlank()) {
            return;
        }

        boolean crateOpenEnabled = plugin.getConfig().getBoolean("discord.events.crate-open", true);
        boolean rareRewardEnabled = plugin.getConfig().getBoolean("discord.events.rare-reward", true);
        double rareThreshold = plugin.getConfig().getDouble("discord.events.rare-threshold", 5.0D);
        boolean isRareReward = reward.getChance() <= rareThreshold;

        if (!force && !crateOpenEnabled && !(rareRewardEnabled && isRareReward)) {
            return;
        }

        String rewardName = reward.getDisplayNameOrFallback();

        String payload = "{"
            + "\"embeds\":[{"
            + "\"title\":\"BloodlineCrates Event\","
            + "\"description\":\"" + escape(player.getName()) + " opened " + escape(crate.getDisplayName()) + "\","
            + "\"fields\":["
            + "{\"name\":\"Player\",\"value\":\"" + escape(player.getName()) + "\",\"inline\":true},"
            + "{\"name\":\"Crate\",\"value\":\"" + escape(crate.getDisplayName()) + "\",\"inline\":true},"
            + "{\"name\":\"Reward\",\"value\":\"" + escape(rewardName) + "\",\"inline\":false},"
            + "{\"name\":\"Chance\",\"value\":\"" + reward.getChance() + "%\",\"inline\":true}"
            + "]"
            + "}]"
            + "}";

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .exceptionally(throwable -> {
                    plugin.getLogger().warning("Discord webhook request failed: " + throwable.getMessage());
                    return null;
                });
        } catch (Exception exception) {
            plugin.getLogger().warning("Discord webhook dispatch failed: " + exception.getMessage());
        }
    }

    private String escape(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n");
    }
}
