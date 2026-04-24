package com.bloodline.crates.reward.executor;

import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.Reward;
import com.bloodline.crates.reward.RewardContext;
import com.bloodline.crates.reward.RewardExecutor;
import com.bloodline.crates.reward.RewardType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class BroadcastRewardExecutor implements RewardExecutor {
    @Override
    public void execute(RewardContext context) {
        Reward reward = context.getReward();
        Crate crate = context.getSourceCrate();
        String message = reward.getBroadcastMessage()
            .replace("{player}", context.getPlayer().getName())
            .replace("{reward}", reward.getDisplayNameOrFallback())
            .replace("{crate}", crate == null ? "Unknown" : crate.getDisplayName());
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public RewardType getType() {
        return RewardType.BROADCAST;
    }

    @Override
    public boolean validate(Reward reward) {
        return reward.getBroadcastMessage() != null && !reward.getBroadcastMessage().isBlank();
    }
}
