package com.bloodline.crates.animation;

import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.Reward;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Data
@AllArgsConstructor
public class AnimationContext {
    private Player player;
    private Crate crate;
    private Reward reward;
    private Location location;
    private Runnable onComplete;
}