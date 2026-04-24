package com.bloodline.crates.reward.executor;

import com.bloodline.crates.model.Reward;
import com.bloodline.crates.reward.RewardContext;
import com.bloodline.crates.reward.RewardExecutor;
import com.bloodline.crates.reward.RewardType;
import com.bloodline.crates.util.CommandRewardUtil;
import org.bukkit.Bukkit;

public class CommandRewardExecutor implements RewardExecutor {
    @Override
    public void execute(RewardContext context) {
        Reward reward = context.getReward();
        String raw = reward.getCommand();
        if (raw == null || raw.isBlank()) {
            return;
        }

        String resolved = CommandRewardUtil.resolveCommand(context.getPlugin(), context.getPlayer(), raw.replaceFirst("^console:", "").replaceFirst("^player:", ""));
        if (raw.toLowerCase().startsWith("console:")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
        } else {
            context.getPlayer().performCommand(resolved);
        }

        context.getPlayer().sendMessage(context.getPlugin().getConfigManager().getPrefix()
            + context.getPlugin().getConfigManager().getMessage("reward-granted").replace("{reward}", reward.getDisplayNameOrFallback()));
    }

    @Override
    public RewardType getType() {
        return RewardType.COMMAND;
    }

    @Override
    public boolean validate(Reward reward) {
        return reward.getCommand() != null && !reward.getCommand().isBlank();
    }
}
