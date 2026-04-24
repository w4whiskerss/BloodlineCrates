package com.bloodline.crates.failsafe;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.model.Reward;
import com.bloodline.crates.reward.RewardContext;
import com.bloodline.crates.util.RewardStorageUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClaimsManager {
    private final BloodlineCrates plugin;
    private final File claimsFolder;

    public ClaimsManager(BloodlineCrates plugin) {
        this.plugin = plugin;
        this.claimsFolder = new File(plugin.getDataFolder(), "failsafe");
        if (!claimsFolder.exists()) {
            claimsFolder.mkdirs();
        }
    }

    public void addClaim(UUID playerId, Reward reward) {
        List<Reward> claims = new ArrayList<>(getClaims(playerId));
        claims.add(reward.copy());
        saveClaims(playerId, claims);
    }

    public List<Reward> getClaims(UUID playerId) {
        File claimsFile = getClaimsFile(playerId);
        if (!claimsFile.exists()) {
            return new ArrayList<>();
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(claimsFile);
        ConfigurationSection section = configuration.getConfigurationSection("claims");
        if (section == null) {
            return new ArrayList<>();
        }

        List<Reward> claims = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection claimSection = section.getConfigurationSection(key);
            if (claimSection == null) {
                continue;
            }

            Reward reward = RewardStorageUtil.readReward(claimSection);
            if (reward != null) {
                claims.add(reward);
            }
        }

        return claims;
    }

    public void removeClaim(UUID playerId, Reward reward) {
        List<Reward> claims = new ArrayList<>(getClaims(playerId));
        for (int index = 0; index < claims.size(); index++) {
            if (areEquivalent(claims.get(index), reward)) {
                claims.remove(index);
                saveClaims(playerId, claims);
                return;
            }
        }
    }

    public boolean redeemClaim(Player player, int claimIndex) {
        List<Reward> claims = new ArrayList<>(getClaims(player.getUniqueId()));
        if (claimIndex < 1 || claimIndex > claims.size()) {
            return false;
        }

        Reward reward = claims.get(claimIndex - 1);
        String transactionId = plugin.getAntiDupeManager().beginTransaction(player.getUniqueId(), reward);

        try {
            plugin.getRewardExecutorRegistry().execute(new RewardContext(player, null, reward, plugin));

            claims.remove(claimIndex - 1);
            saveClaims(player.getUniqueId(), claims);
            plugin.getAntiDupeManager().completeTransaction(player.getUniqueId(), transactionId);
            player.sendMessage(plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().getMessage("claim-redeemed").replace("{reward}", getRewardName(reward)));
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to redeem claim for " + player.getName() + ": " + exception.getMessage());
            return false;
        }
    }

    private void saveClaims(UUID playerId, List<Reward> claims) {
        YamlConfiguration configuration = new YamlConfiguration();
        for (int index = 0; index < claims.size(); index++) {
            RewardStorageUtil.writeReward(configuration.createSection("claims." + index), claims.get(index));
        }

        try {
            configuration.save(getClaimsFile(playerId));
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save claims for " + playerId + ": " + exception.getMessage());
        }
    }

    private File getClaimsFile(UUID playerId) {
        return new File(claimsFolder, playerId + ".yml");
    }

    private boolean areEquivalent(Reward first, Reward second) {
        return first.getChance() == second.getChance()
            && String.valueOf(first.getRequiredPermission()).equals(String.valueOf(second.getRequiredPermission()))
            && String.valueOf(first.getCommand()).equals(String.valueOf(second.getCommand()))
            && first.getRewardType() == second.getRewardType()
            && String.valueOf(first.getCrateId()).equals(String.valueOf(second.getCrateId()))
            && String.valueOf(first.getBroadcastMessage()).equals(String.valueOf(second.getBroadcastMessage()))
            && first.getEconomyAmount() == second.getEconomyAmount()
            && ((first.getItem() == null && second.getItem() == null)
            || (first.getItem() != null && second.getItem() != null && first.getItem().isSimilar(second.getItem())));
    }

    private String getRewardName(Reward reward) {
        return reward.getDisplayNameOrFallback();
    }
}
