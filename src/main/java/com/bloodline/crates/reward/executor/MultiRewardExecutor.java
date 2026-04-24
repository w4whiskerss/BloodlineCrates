package com.bloodline.crates.reward.executor;

import com.bloodline.crates.model.Reward;
import com.bloodline.crates.reward.RewardContext;
import com.bloodline.crates.reward.RewardExecutor;
import com.bloodline.crates.reward.RewardType;

public class MultiRewardExecutor implements RewardExecutor {
    @Override
    public void execute(RewardContext context) {
        for (Reward subReward : context.getReward().getSubRewards()) {
            context.getPlugin().getRewardExecutorRegistry().execute(
                new RewardContext(context.getPlayer(), context.getSourceCrate(), subReward, context.getPlugin())
            );
        }
    }

    @Override
    public RewardType getType() {
        return RewardType.MULTI;
    }

    @Override
    public boolean validate(Reward reward) {
        return reward.getSubRewards() != null && !reward.getSubRewards().isEmpty();
    }
}
