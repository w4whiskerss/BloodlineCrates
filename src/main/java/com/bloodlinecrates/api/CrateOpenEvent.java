package com.bloodlinecrates.api;

import com.bloodlinecrates.model.CrateDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class CrateOpenEvent extends Event implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;
    private final CrateDefinition crate;
    private final boolean testMode;
    private boolean cancelled;

    public CrateOpenEvent(Player player, CrateDefinition crate, boolean testMode) {
        this.player = player;
        this.crate = crate;
        this.testMode = testMode;
    }

    public Player getPlayer() {
        return player;
    }

    public CrateDefinition getCrate() {
        return crate;
    }

    public boolean isTestMode() {
        return testMode;
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
