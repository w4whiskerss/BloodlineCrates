package com.bloodline.crates.util;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.debug.DebugEventType;
import com.bloodline.crates.model.Reward;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RNGUtil {
    
    public static Reward selectReward(BloodlineCrates plugin, List<Reward> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            throw new IllegalArgumentException("Rewards list cannot be null or empty");
        }
        
        double totalWeight = rewards.stream()
            .mapToDouble(Reward::getChance)
            .sum();
        
        if (totalWeight <= 0) {
            throw new IllegalArgumentException("Total weight must be greater than 0");
        }
        
        double random = ThreadLocalRandom.current().nextDouble(totalWeight);
        double currentWeight = 0;
        
        for (Reward reward : rewards) {
            currentWeight += reward.getChance();
            if (random < currentWeight) {
                Reward selected = reward;
                plugin.getDebugManager().emit(DebugEventType.RNG_ROLL, null,
                    () -> "roll=" + random + "/" + totalWeight + ", reward=" + selected.getId() + ", chance=" + selected.getChance());
                return reward;
            }
        }

        Reward selected = rewards.get(rewards.size() - 1);
        plugin.getDebugManager().emit(DebugEventType.RNG_ROLL, null,
            () -> "roll=" + random + "/" + totalWeight + ", reward=" + selected.getId() + ", chance=" + selected.getChance());
        return selected;
    }
}
