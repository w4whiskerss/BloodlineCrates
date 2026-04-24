package com.bloodline.crates.loadout;

import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.CrateType;
import com.bloodline.crates.model.Reward;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class LoadoutValidator {
    private LoadoutValidator() {
    }

    public static void validate(LoadoutData data) throws LoadoutValidationException {
        if (data == null) {
            throw new LoadoutValidationException("Loadout data is missing.");
        }
        if (data.pluginVersion() == null || data.pluginVersion().isBlank()) {
            throw new LoadoutValidationException("pluginVersion is required.");
        }
        if (data.crates() == null || data.crates().isEmpty()) {
            throw new LoadoutValidationException("Loadout must contain at least one crate.");
        }

        for (Crate crate : data.crates()) {
            if (crate.getId() == null || crate.getId().isBlank()) {
                throw new LoadoutValidationException("Crate id cannot be empty.");
            }
            if (crate.getKeyId() == null || crate.getKeyId().isBlank()) {
                throw new LoadoutValidationException("Crate keyId cannot be empty for crate " + crate.getId() + ".");
            }
            if (crate.getRewards() == null || crate.getRewards().isEmpty()) {
                throw new LoadoutValidationException("Crate " + crate.getId() + " must contain rewards.");
            }

            double totalChance = 0.0D;
            for (Reward reward : crate.getRewards()) {
                if (reward.getChance() <= 0.0D || reward.getChance() > 100.0D) {
                    throw new LoadoutValidationException("Reward chance must be > 0 and <= 100 for crate " + crate.getId() + ".");
                }
                ItemStack item = reward.getItem();
                if (item == null || item.getType() == Material.AIR) {
                    throw new LoadoutValidationException("Reward item cannot be null or AIR for crate " + crate.getId() + ".");
                }
                totalChance += reward.getChance();
            }

            if (crate.getType() == CrateType.RANDOM && totalChance > 100.0D) {
                throw new LoadoutValidationException("Random crate " + crate.getId() + " exceeds 100 total chance.");
            }
        }
    }
}
