package com.bloodline.crates.discord;

import com.bloodline.crates.BloodlineCrates;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.time.Instant;

public class DiscordEmbedBuilder {
    public static final Color COLOUR_SUCCESS = new Color(0x2ECC71);
    public static final Color COLOUR_ERROR = new Color(0xE74C3C);
    public static final Color COLOUR_INFO = new Color(0x3498DB);
    public static final Color COLOUR_ALERT = new Color(0xF39C12);
    public static final Color COLOUR_RARE = new Color(0x9B59B6);

    private final BloodlineCrates plugin;

    public DiscordEmbedBuilder(BloodlineCrates plugin) {
        this.plugin = plugin;
    }

    public MessageEmbed success(String title, String description) {
        return base("BloodlineCrates - " + title, description, COLOUR_SUCCESS).build();
    }

    public MessageEmbed error(String description) {
        return base("BloodlineCrates - Error", description, COLOUR_ERROR).build();
    }

    public MessageEmbed info(String title, String description) {
        return base("BloodlineCrates - " + title, description, COLOUR_INFO).build();
    }

    public MessageEmbed alert(String title, String description) {
        return base("BloodlineCrates - " + title, description, COLOUR_ALERT).build();
    }

    public MessageEmbed rare(String title, String description) {
        return base("BloodlineCrates - " + title, description, COLOUR_RARE).build();
    }

    public EmbedBuilder base(String title, String description, Color colour) {
        return new EmbedBuilder()
            .setColor(colour)
            .setTitle(title)
            .setDescription(description)
            .setFooter("BloodlineCrates • " + plugin.getConfig().getString("server-name", "Server"), null)
            .setTimestamp(Instant.now());
    }
}
