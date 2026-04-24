package com.bloodline.crates.discord.listener;

import com.bloodline.crates.discord.DiscordBotManager;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class BotReadyListener extends ListenerAdapter {
    private final DiscordBotManager manager;

    public BotReadyListener(DiscordBotManager manager) {
        this.manager = manager;
    }

    @Override
    public void onReady(ReadyEvent event) {
        manager.onReady(event.getJDA());
    }
}
