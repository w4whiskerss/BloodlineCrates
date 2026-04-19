package com.bloodlinecrates.util;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;

public final class ItemStackSerializer {
    static {
        ConfigurationSerialization.registerClass(ItemStack.class);
    }

    private ItemStackSerializer() {
    }
}
