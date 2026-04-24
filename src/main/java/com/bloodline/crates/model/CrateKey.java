package com.bloodline.crates.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.inventory.ItemStack;

@Data
@AllArgsConstructor
public class CrateKey {
    private String crateId;
    private ItemStack displayItem;
}