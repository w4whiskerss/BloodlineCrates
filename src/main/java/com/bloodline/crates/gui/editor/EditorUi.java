package com.bloodline.crates.gui.editor;

import com.bloodline.crates.util.ItemBuilder;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class EditorUi {
    private EditorUi() {
    }

    public static void fill(Inventory inventory) {
        ItemStack filler = new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE)
            .name(" ")
            .build();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }
}
