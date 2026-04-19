package com.bloodlinecrates.api;

import com.bloodlinecrates.model.CrateDefinition;
import com.bloodlinecrates.model.CrateReward;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class RewardGiveEvent extends Event implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;
    private final CrateDefinition crate;
    private final CrateReward reward;
    private boolean cancelled;

    public RewardGiveEvent(Player player, CrateDefinition crate, CrateReward reward) {
        this.player = player;
        this.crate = crate;
        this.reward = reward;
    }

    public Player getPlayer() {
        return player;
    }

    public CrateDefinition getCrate() {
        return crate;
    }

    public CrateReward getReward() {
        return reward;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
