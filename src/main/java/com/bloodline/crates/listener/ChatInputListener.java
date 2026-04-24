package com.bloodline.crates.listener;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatInputListener implements Listener {
    private final BloodlineCrates plugin;
    
    public ChatInputListener(BloodlineCrates plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getGuiManager().getInputHandler().hasPendingInput(player.getUniqueId())) {
            event.setCancelled(true);
            String input = event.getMessage();
            plugin.getGuiManager().getInputHandler().handleInput(player.getUniqueId(), input);
        }
    }
}