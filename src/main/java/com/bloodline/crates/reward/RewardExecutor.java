package com.bloodline.crates.reward;

import com.bloodline.crates.model.Reward;

public interface RewardExecutor {
    void execute(RewardContext context);

    RewardType getType();

    boolean validate(Reward reward);
}
