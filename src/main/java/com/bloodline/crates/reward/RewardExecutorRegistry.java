package com.bloodline.crates.reward;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.debug.DebugEventType;
import com.bloodline.crates.model.Reward;

import java.util.EnumMap;
import java.util.Map;

public class RewardExecutorRegistry {
    private final BloodlineCrates plugin;
    private final Map<RewardType, RewardExecutor> executors = new EnumMap<>(RewardType.class);

    public RewardExecutorRegistry(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    public void register(RewardExecutor executor) {
        executors.put(executor.getType(), executor);
    }

    public void execute(RewardContext context) {
        Reward reward = context.getReward();
        plugin.getDebugManager().emit(DebugEventType.REWARD_EXECUTE, context.getPlayer().getUniqueId(),
            () -> "crate=" + context.getSourceCrate().getId() + ", reward=" + reward.getId() + ", type=" + reward.getRewardType());
        RewardExecutor executor = executors.get(reward.getRewardType());
        if (executor == null) {
            plugin.getLogger().warning("No reward executor registered for type " + reward.getRewardType() + ".");
            return;
        }
        if (!executor.validate(reward)) {
            plugin.getLogger().warning("Skipped malformed reward " + reward.getId() + " of type " + reward.getRewardType() + ".");
            return;
        }
        executor.execute(context);
    }
}
