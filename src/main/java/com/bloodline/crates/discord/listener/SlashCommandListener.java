package com.bloodline.crates.discord.listener;

import com.bloodline.crates.discord.DiscordBotManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SlashCommandListener extends ListenerAdapter {
    private final DiscordBotManager manager;

    public SlashCommandListener(DiscordBotManager manager) {
        this.manager = manager;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        manager.getCommandRouter().handleSlash(event);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        manager.getCommandRouter().handleButton(event);
    }
}
