package com.bloodline.crates.manager;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.audit.AuditEntry;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.CrateType;
import com.bloodline.crates.model.KeyMode;
import com.bloodline.crates.model.Reward;
import com.bloodline.crates.reward.RewardContext;
import com.bloodline.crates.util.ItemBuilder;
import com.bloodline.crates.util.RNGUtil;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CrateManager {
    private final BloodlineCrates plugin;

    public CrateManager(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    public void registerCrateLocation(Location location, String crateId) {
        plugin.getPlacementManager().placeCrate(null, crateId, location);
    }

    public void unregisterCrateLocation(Location location) {
        plugin.getPlacementManager().removeCrate(location);
    }

    public Optional<String> getCrateIdAtLocation(Location location) {
        return plugin.getPlacementManager().getCrateAt(location).map(placedCrate -> placedCrate.getCrateId());
    }

    public void handleCrateInteraction(Player player, Location location) {
        if (plugin.getWorldBlacklist().isBlacklisted(player.getWorld())) {
            plugin.getWorldBlacklist().notifyBlocked(player);
            return;
        }

        Optional<String> crateIdOpt = getCrateIdAtLocation(location);
        if (crateIdOpt.isEmpty()) {
            return;
        }

        String crateId = crateIdOpt.get();
        Optional<Crate> crateOpt = plugin.getConfigManager().getCrate(crateId);
        if (crateOpt.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "\u00A7cThis crate is not configured properly.");
            return;
        }

        Crate crate = crateOpt.get();
        com.bloodline.crates.limit.LimitManager.LimitCheckResult limitCheck = plugin.getLimitManager().canOpen(player, crate);
        if (!limitCheck.allowed()) {
            if (limitCheck.message() != null && !limitCheck.message().isBlank()) {
                player.sendMessage(plugin.getConfigManager().getPrefix()
                    + ChatColor.translateAlternateColorCodes('&', limitCheck.message()));
            }
            return;
        }
        if (!canOpen(player, crate)) {
            return;
        }

        if (crate.getType() == CrateType.SELECTABLE) {
            plugin.getGuiManager().openSelectable(player, crate, location);
        } else {
            openRandomCrate(player, crate, location);
        }
    }

    public ItemStack createCrateItem(Crate crate, int amount) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Crate ID: &e" + crate.getId());
        if (!crate.getDescriptionLines().isEmpty()) {
            lore.add("");
            lore.addAll(crate.getDescriptionLines());
        }
        lore.add("");
        lore.add("&7Key Mode: &f" + crate.getKeyMode().name());
        lore.add("&eRight-click to place this crate!");
        return new ItemBuilder(XMaterial.CHEST)
            .name(crate.getDisplayName())
            .lore(lore)
            .amount(amount)
            .build();
    }

    public void openRandomCrate(Player player, Crate crate, Location location) {
        List<Reward> eligibleRewards = new ArrayList<>();
        for (Reward reward : crate.getRewards()) {
            if (!reward.hasPermission() || player.hasPermission(reward.getRequiredPermission())) {
                eligibleRewards.add(reward);
            }
        }

        if (eligibleRewards.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "\u00A7cNo rewards available for you!");
            return;
        }

        try {
            Optional<Reward> pityReward = plugin.getPityManager().checkPity(player, crate)
                .filter(eligibleRewards::contains);
            Reward selectedReward = pityReward.orElseGet(() -> RNGUtil.selectReward(plugin, eligibleRewards));
            if (!plugin.getKeyManager().consumeKey(player, crate)) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + formatMissingKeyMessage(crate));
                return;
            }

            openSelectedCrate(player, crate, selectedReward, location, pityReward.isPresent());
        } catch (Exception e) {
            plugin.getLogger().severe("Error opening random crate: " + e.getMessage());
            player.sendMessage(plugin.getConfigManager().getPrefix() + "\u00A7cAn error occurred while opening the crate.");
        }
    }

    public void openSelectedCrate(Player player, Crate crate, Reward reward, Location location) {
        openSelectedCrate(player, crate, reward, location, false);
    }

    public void openSelectedCrate(Player player, Crate crate, Reward reward, Location location, boolean pityTriggered) {
        if (plugin.getLeaderboardManager() != null && plugin.getLeaderboardManager().isEnabled()) {
            plugin.getLeaderboardManager().recordOpen(player.getUniqueId(), player.getName());
        }

        plugin.getAuditLogger().log(new AuditEntry(
            player.getUniqueId(),
            player.getName(),
            AuditEntry.AuditEventType.CRATE_OPEN,
            crate.getId(),
            "Reward=" + reward.getId(),
            System.currentTimeMillis()
        ));

        Location animationLocation = location != null ? location : player.getLocation();
        Runnable onComplete = () -> grantReward(player, crate, reward, location, pityTriggered);

        com.bloodline.crates.animation.AnimationContext context =
            new com.bloodline.crates.animation.AnimationContext(
                player, crate, reward, animationLocation, onComplete
            );

        plugin.getAnimationManager().playAnimation(context);
    }

    public void grantReward(Player player, Crate crate, Reward reward) {
        grantReward(player, crate, reward, null, false);
    }

    public void grantReward(Player player, Crate crate, Reward reward, Location location) {
        grantReward(player, crate, reward, location, false);
    }

    public void grantReward(Player player, Crate crate, Reward reward, Location location, boolean pityTriggered) {
        String transactionId = plugin.getAntiDupeManager().beginTransaction(player.getUniqueId(), reward);

        try {
            plugin.getRewardExecutorRegistry().execute(new RewardContext(player, crate, reward, plugin));
            plugin.getLimitManager().recordOpen(player, crate);
            int playerOpens = plugin.getPlayerDataManager().getCrateOpenCount(player.getUniqueId(), crate.getId());
            int serverOpens = plugin.getBroadcastManager().incrementServerOpens(crate.getId());
            plugin.getBroadcastManager().checkAndBroadcast(player, crate, reward, pityTriggered, playerOpens, serverOpens);

            boolean pityJackpot = crate.isPityEnabled()
                && reward.getId() != null
                && reward.getId().equalsIgnoreCase(crate.getJackpotRewardId());
            if (pityJackpot) {
                plugin.getPityManager().resetPity(player.getUniqueId(), crate.getId());
            } else {
                plugin.getPityManager().incrementPity(player.getUniqueId(), crate.getId());
            }
            plugin.getStatsManager().recordOpen(player.getUniqueId(), player.getName(), crate, reward);

            plugin.getAntiDupeManager().completeTransaction(player.getUniqueId(), transactionId);
            if (crate.isCooldownEnabled()) {
                plugin.getCooldownManager().applyCooldown(player.getUniqueId(), crate);
            }
            if (location != null && plugin.getPlacementManager() != null) {
                plugin.getPlacementManager().recordOpen(location);
            }
            plugin.getDiscordWebhookManager().sendCrateOpenEvent(player, crate, reward);
            if (plugin.getSyncManager() != null) {
                plugin.getSyncManager().publishRewardGrant(player.getUniqueId(), crate.getId(), getRewardName(reward));
            }
            plugin.getAuditLogger().log(new AuditEntry(
                player.getUniqueId(),
                player.getName(),
                AuditEntry.AuditEventType.REWARD_GRANTED,
                crate.getId(),
                getRewardName(reward),
                System.currentTimeMillis()
            ));
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to grant reward to " + player.getName() + ": " + exception.getMessage());
        }
    }

    private String getRewardName(Reward reward) {
        return reward.getDisplayNameOrFallback();
    }

    public boolean canOpen(Player player, Crate crate) {
        if (crate.isCooldownEnabled()) {
            String bypassPermission = crate.getCooldownBypassPermission();
            boolean bypass = bypassPermission != null
                && !bypassPermission.isBlank()
                && player.isPermissionSet(bypassPermission)
                && player.hasPermission(bypassPermission);
            if (!bypass) {
                boolean onCooldown = plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), crate);
                if (onCooldown) {
                    long remainingMillis = plugin.getCooldownManager().getRemainingMillis(player.getUniqueId(), crate);
                    String message = crate.getCooldownMessage() == null || crate.getCooldownMessage().isBlank()
                        ? "&cYou must wait &e{time} &cbefore opening this crate again."
                        : crate.getCooldownMessage();
                    player.sendMessage(plugin.getConfigManager().getPrefix()
                        + ChatColor.translateAlternateColorCodes('&', message.replace("{time}", plugin.getCooldownManager().formatRemaining(remainingMillis))));
                    return false;
                }
            }
        }

        if (!plugin.getKeyManager().hasKey(player, crate)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + formatMissingKeyMessage(crate));
            return false;
        }
        return true;
    }

    private String formatMissingKeyMessage(Crate crate) {
        if (crate.getKeyMode() == KeyMode.VIRTUAL) {
            return "\u00A7cYou need a virtual key to open this crate!";
        }
        return "\u00A7cYou need a physical key to open this crate!";
    }

    private String missingKeyMessage(Crate crate) {
        if (crate.getKeyMode() == KeyMode.VIRTUAL) {
            return "§cYou need a virtual key to open this crate!";
        }
        return "§cYou need a physical key to open this crate!";
    }

    public void saveCrateLocations() {
        // Legacy no-op: placement persistence is now handled by PlacementManager.
    }
}
