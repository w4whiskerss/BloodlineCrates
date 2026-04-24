package com.bloodline.crates.animation;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.debug.DebugEventType;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.particles.XParticle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class AnimationManager {
    private final BloodlineCrates plugin;
    
    public AnimationManager(BloodlineCrates plugin) {
        this.plugin = plugin;
    }
    
    public void playAnimation(AnimationContext context) {
        AnimationConfig config = context.getCrate().getAnimationConfig();
        plugin.getDebugManager().emit(DebugEventType.ANIMATION_START, context.getPlayer().getUniqueId(),
            () -> "crate=" + context.getCrate().getId() + ", reward=" + context.getReward().getId() + ", type=" + config.getType());
        
        if (config.getType() == AnimationType.INSTANT) {
            playInstantAnimation(context);
        } else if (config.getType() == AnimationType.ROULETTE) {
            playRouletteAnimation(context);
        } else if (config.getType() == AnimationType.CSGO) {
            playCSGOAnimation(context);
        }
    }
    
    private void playInstantAnimation(AnimationContext context) {
        Player player = context.getPlayer();
        AnimationConfig config = context.getCrate().getAnimationConfig();
        
        XSound.matchXSound(config.getOpenSound()).ifPresent(s -> s.play(player));
        
        spawnParticles(context.getLocation(), config);
        
        context.getOnComplete().run();
        plugin.getDebugManager().emit(DebugEventType.ANIMATION_END, player.getUniqueId(),
            () -> "crate=" + context.getCrate().getId() + ", reward=" + context.getReward().getId() + ", type=" + config.getType());
    }
    
    private void playRouletteAnimation(AnimationContext context) {
        Player player = context.getPlayer();
        AnimationConfig config = context.getCrate().getAnimationConfig();
        
        XSound.matchXSound(config.getOpenSound()).ifPresent(s -> s.play(player));
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= config.getDurationTicks()) {
                    XSound.matchXSound(config.getRevealSound()).ifPresent(s -> s.play(player));
                    spawnParticles(context.getLocation(), config);
                    context.getOnComplete().run();
                    plugin.getDebugManager().emit(DebugEventType.ANIMATION_END, player.getUniqueId(),
                        () -> "crate=" + context.getCrate().getId() + ", reward=" + context.getReward().getId() + ", type=" + config.getType());
                    cancel();
                    return;
                }
                
                if (ticks % 10 == 0) {
                    spawnParticles(context.getLocation(), config);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void playCSGOAnimation(AnimationContext context) {
        Player player = context.getPlayer();
        AnimationConfig config = context.getCrate().getAnimationConfig();
        
        XSound.matchXSound(config.getOpenSound()).ifPresent(s -> s.play(player));
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= config.getDurationTicks()) {
                    XSound.matchXSound(config.getRevealSound()).ifPresent(s -> s.play(player));
                    spawnParticles(context.getLocation(), config);
                    context.getOnComplete().run();
                    plugin.getDebugManager().emit(DebugEventType.ANIMATION_END, player.getUniqueId(),
                        () -> "crate=" + context.getCrate().getId() + ", reward=" + context.getReward().getId() + ", type=" + config.getType());
                    cancel();
                    return;
                }
                
                if (ticks % 5 == 0) {
                    spawnParticles(context.getLocation(), config);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void spawnParticles(Location location, AnimationConfig config) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        
        Location spawnLoc = location.clone().add(0.5, 1.0, 0.5);
        
        XParticle.of(config.getParticleType()).ifPresent(particle -> {
            location.getWorld().spawnParticle(
                particle.get(), 
                spawnLoc, 
                config.getParticleCount(),
                0.3, 0.3, 0.3,
                0.05
            );
        });
    }
}
