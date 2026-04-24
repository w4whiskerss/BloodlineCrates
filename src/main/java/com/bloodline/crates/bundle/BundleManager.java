package com.bloodline.crates.bundle;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.model.Crate;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

public class BundleManager {
    private final BloodlineCrates plugin;
    private final Map<String, CrateBundle> bundles = new HashMap<>();

    public BundleManager(BloodlineCrates plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        bundles.clear();

        ConfigurationSection bundlesSection = plugin.getConfig().getConfigurationSection("bundles");
        if (bundlesSection == null) {
            return;
        }

        for (String bundleId : bundlesSection.getKeys(false)) {
            ConfigurationSection bundleSection = bundlesSection.getConfigurationSection(bundleId);
            if (bundleSection == null) {
                continue;
            }

            String displayName = ChatColor.translateAlternateColorCodes(
                '&',
                bundleSection.getString("displayName", bundleId)
            );

            List<CrateBundle.BundleEntry> entries = new ArrayList<>();
            List<Map<?, ?>> contents = bundleSection.getMapList("contents");
            for (Map<?, ?> rawEntry : contents) {
                String crateId = String.valueOf(rawEntry.get("crate-id"));
                int amount = parseAmount(rawEntry.get("amount"));
                if (crateId == null || crateId.isBlank()) {
                    continue;
                }
                entries.add(new CrateBundle.BundleEntry(crateId, Math.max(1, amount)));
            }

            bundles.put(bundleId.toLowerCase(Locale.ROOT), new CrateBundle(bundleId, displayName, entries));
        }
    }

    public Optional<CrateBundle> getBundle(String bundleId) {
        return Optional.ofNullable(bundles.get(bundleId.toLowerCase(Locale.ROOT)));
    }

    public Collection<CrateBundle> getBundles() {
        return Collections.unmodifiableCollection(bundles.values());
    }

    public boolean giveBundle(Player player, String bundleId) {
        Optional<CrateBundle> bundleOptional = getBundle(bundleId);
        if (bundleOptional.isEmpty()) {
            return false;
        }

        CrateBundle bundle = bundleOptional.get();
        for (CrateBundle.BundleEntry entry : bundle.getContents()) {
            Optional<Crate> crateOptional = plugin.getConfigManager().getCrate(entry.getCrateId());
            if (crateOptional.isEmpty()) {
                plugin.getLogger().warning("Bundle " + bundleId + " references unknown crate " + entry.getCrateId());
                continue;
            }

            player.getInventory().addItem(plugin.getCrateManager().createCrateItem(crateOptional.get(), entry.getAmount()));
        }

        return true;
    }

    private int parseAmount(Object rawAmount) {
        if (rawAmount instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(rawAmount));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }
}
