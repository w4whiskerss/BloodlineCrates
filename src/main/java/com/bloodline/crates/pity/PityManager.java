package com.bloodline.crates.pity;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.debug.DebugEventType;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.Reward;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class PityManager {
    private final BloodlineCrates plugin;

    public PityManager(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    public Optional<Reward> checkPity(Player player, Crate crate) {
        if (!crate.isPityEnabled() || crate.getPityThreshold() <= 0 || crate.getJackpotRewardId() == null || crate.getJackpotRewardId().isBlank()) {
            plugin.getDebugManager().emit(DebugEventType.PITY_CHECK, player.getUniqueId(),
                () -> "crate=" + crate.getId() + ", enabled=false, reward=none");
            return Optional.empty();
        }

        int currentCount = getPityCount(player.getUniqueId(), crate.getId());
        if (currentCount < crate.getPityThreshold()) {
            plugin.getDebugManager().emit(DebugEventType.PITY_CHECK, player.getUniqueId(),
                () -> "crate=" + crate.getId() + ", count=" + currentCount + "/" + crate.getPityThreshold() + ", reward=none");
            return Optional.empty();
        }

        Optional<Reward> result = crate.getRewards().stream()
            .filter(reward -> crate.getJackpotRewardId().equalsIgnoreCase(reward.getId()))
            .findFirst();
        plugin.getDebugManager().emit(DebugEventType.PITY_CHECK, player.getUniqueId(),
            () -> "crate=" + crate.getId() + ", count=" + currentCount + "/" + crate.getPityThreshold()
                + ", reward=" + result.map(Reward::getId).orElse("none"));
        return result;
    }

    public int getPityCount(UUID playerId, String crateId) {
        return plugin.getPlayerDataManager().getPityCount(playerId, crateId);
    }

    public void incrementPity(UUID playerId, String crateId) {
        plugin.getPlayerDataManager().setPityCount(playerId, crateId, getPityCount(playerId, crateId) + 1);
    }

    public void resetPity(UUID playerId, String crateId) {
        plugin.getPlayerDataManager().setPityCount(playerId, crateId, 0);
    }
}
