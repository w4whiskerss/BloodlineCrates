package com.bloodline.crates.reward.executor;

import com.bloodline.crates.model.Reward;
import com.bloodline.crates.reward.RewardContext;
import com.bloodline.crates.reward.RewardExecutor;
import com.bloodline.crates.reward.RewardType;

public class EconomyRewardExecutor implements RewardExecutor {
    @Override
    public void execute(RewardContext context) {
        context.getPlugin().getVaultEconomyHook().grantEconomyReward(context.getPlayer(), context.getReward().getEconomyAmount());
        context.getPlayer().sendMessage(context.getPlugin().getConfigManager().getPrefix()
            + context.getPlugin().getConfigManager().getMessage("reward-granted").replace("{reward}", context.getReward().getDisplayNameOrFallback()));
    }

    @Override
    public RewardType getType() {
        return RewardType.ECONOMY;
    }

    @Override
    public boolean validate(Reward reward) {
        return reward.getEconomyAmount() > 0.0D;
    }
}
