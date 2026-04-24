package com.bloodline.crates.economy;

import com.bloodline.crates.BloodlineCrates;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultEconomyHook {
    private final BloodlineCrates plugin;
    private Economy economy;

    public VaultEconomyHook(BloodlineCrates plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public void grantEconomyReward(Player player, double amount) {
        if (!isAvailable()) {
            return;
        }

        try {
            economy.depositPlayer(player, amount);
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to deposit economy reward for " + player.getName() + ": " + exception.getMessage());
        }
    }

    private void setupEconomy() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return;
        }

        RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration != null) {
            this.economy = registration.getProvider();
        }
    }
}
