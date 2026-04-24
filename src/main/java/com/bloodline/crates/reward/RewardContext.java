package com.bloodline.crates.reward;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.Reward;
import lombok.Getter;
import org.bukkit.entity.Player;

@Getter
public class RewardContext {
    private final Player player;
    private final Crate sourceCrate;
    private final Reward reward;
    private final BloodlineCrates plugin;

    public RewardContext(Player player, Crate sourceCrate, Reward reward, BloodlineCrates plugin) {
        this.player = player;
        this.sourceCrate = sourceCrate;
        this.reward = reward;
        this.plugin = plugin;
    }
}
