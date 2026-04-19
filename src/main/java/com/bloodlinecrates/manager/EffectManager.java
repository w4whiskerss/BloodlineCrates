package com.bloodlinecrates.manager;

import com.bloodlinecrates.model.AnimationType;
import com.bloodlinecrates.model.CrateDefinition;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class EffectManager {
    private final JavaPlugin plugin;

    public EffectManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void playOpenEffects(Player player, CrateDefinition crate) {
        boolean skip = player.hasPermission(crate.effectSettings().skipPermission()) && player.isSneaking();
        if (skip) {
            return;
        }
        switch (crate.effectSettings().animationType()) {
            case SPIN, PROGRESSIVE_REVEAL -> plugin.getServer().getScheduler().runTaskLater(plugin, () -> perform(player, crate), crate.effectSettings().durationTicks());
            case MYSTERY_BOX -> plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
                int tick = 0;

                @Override
                public void run() {
                    if (tick >= crate.effectSettings().durationTicks()) {
                        perform(player, crate);
                        return;
                    }
                    tick += crate.effectSettings().speedTicks();
                    if (crate.effectSettings().particles()) {
                        player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 8);
                    }
                }
            }, 0L, Math.max(1, crate.effectSettings().speedTicks()));
            case INSTANT_REVEAL -> perform(player, crate);
        }
    }

    private void perform(Player player, CrateDefinition crate) {
        if (crate.effectSettings().particles()) {
            player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1.0D, 0), 24);
        }
        if (crate.effectSettings().sounds()) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.2F);
        }
    }
}
