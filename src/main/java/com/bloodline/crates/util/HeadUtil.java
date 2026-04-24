package com.bloodline.crates.util;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

public final class HeadUtil {
    private HeadUtil() {
    }

    public static ItemStack createCustomHead(String textureHash, XMaterial fallbackMaterial, String name, List<String> lore) {
        ItemStack fallback = createFallbackItem(fallbackMaterial, name, lore);
        if (textureHash == null || textureHash.isBlank()) {
            return fallback;
        }

        ItemStack head = new ItemBuilder(XMaterial.PLAYER_HEAD)
            .name(name)
            .lore(lore)
            .build();
        if (head == null) {
            return fallback;
        }

        try {
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta == null) {
                return fallback;
            }

            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.nameUUIDFromBytes(textureHash.getBytes()));
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(URI.create("https://textures.minecraft.net/texture/" + textureHash).toURL());
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            head.setItemMeta(meta);
            return head;
        } catch (MalformedURLException | ClassCastException ignored) {
            return fallback;
        }
    }

    private static ItemStack createFallbackItem(XMaterial fallbackMaterial, String name, List<String> lore) {
        XMaterial material = fallbackMaterial == null ? XMaterial.PAPER : fallbackMaterial;
        ItemStack fallback = new ItemBuilder(material)
            .name(name)
            .lore(lore)
            .build();
        if (fallback != null) {
            return fallback;
        }
        return new ItemBuilder(XMaterial.PAPER)
            .name(name)
            .lore(lore)
            .build();
    }
}
