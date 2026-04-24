package com.bloodline.crates.reward.executor;

import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.Reward;
import com.bloodline.crates.reward.RewardContext;
import com.bloodline.crates.reward.RewardExecutor;
import com.bloodline.crates.reward.RewardType;

public class CrateRewardExecutor implements RewardExecutor {
    @Override
    public void execute(RewardContext context) {
        Reward reward = context.getReward();
        Crate crate = context.getPlugin().getConfigManager().getCrate(reward.getCrateId()).orElse(null);
        if (crate == null) {
            return;
        }
        context.getPlayer().getInventory().addItem(context.getPlugin().getCrateManager().createCrateItem(crate, 1));
        context.getPlayer().sendMessage(context.getPlugin().getConfigManager().getPrefix()
            + context.getPlugin().getConfigManager().getMessage("reward-granted").replace("{reward}", reward.getDisplayNameOrFallback()));
    }

    @Override
    public RewardType getType() {
        return RewardType.CRATE;
    }

    @Override
    public boolean validate(Reward reward) {
        return reward.getCrateId() != null && !reward.getCrateId().isBlank();
    }
}
