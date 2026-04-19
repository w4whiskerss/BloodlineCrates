package com.bloodlinecrates.manager;

import com.bloodlinecrates.BloodlineCratesPlugin;
import com.bloodlinecrates.model.CrateKey;
import com.bloodlinecrates.model.PlayerProfile;
import com.bloodlinecrates.model.TimedKeyRule;
import com.bloodlinecrates.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class KeyManager {
    private final JavaPlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final Map<String, CrateKey> keys = new HashMap<>();

    public KeyManager(JavaPlugin plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    public void load(FileConfiguration configuration) {
        keys.clear();
        ConfigurationSection section = configuration.getConfigurationSection("keys");
        if (section == null) {
            return;
        }
        for (String keyId : section.getKeys(false)) {
            ConfigurationSection keySection = section.getConfigurationSection(keyId);
            if (keySection == null) {
                continue;
            }
            CrateKey crateKey = new CrateKey(keyId);
            crateKey.setDisplayName(keySection.getString("display_name", keyId));
            crateKey.setLore(keySection.getStringList("lore"));
            crateKey.setItemsAdderModel(keySection.getString("itemsadder_model", ""));
            crateKey.setPhysicalEnabled(keySection.getBoolean("physical_enabled", true));
            crateKey.setVirtualEnabled(keySection.getBoolean("virtual_enabled", true));
            crateKey.setItem(buildItem(keySection.getConfigurationSection("item"), crateKey.displayName(), crateKey.lore()));
            for (Map<?, ?> map : keySection.getMapList("timed_rewards")) {
                Number intervalValue = map.containsKey("interval_minutes") ? (Number) map.get("interval_minutes") : 60;
                Number amountValue = map.containsKey("amount") ? (Number) map.get("amount") : 1;
                int intervalMinutes = intervalValue.intValue();
                int amount = amountValue.intValue();
                @SuppressWarnings("unchecked")
                Map<String, Double> multipliers = map.containsKey("permission_multiplier")
                        ? (Map<String, Double>) map.get("permission_multiplier")
                        : Map.of();
                crateKey.timedKeyRules().add(new TimedKeyRule(intervalMinutes, amount, multipliers, false));
            }
            keys.put(crateKey.id(), crateKey);
        }
    }

    public Optional<CrateKey> get(String id) {
        return Optional.ofNullable(keys.get(id.toLowerCase()));
    }

    public Collection<CrateKey> all() {
        return keys.values();
    }

    public void register(CrateKey key) {
        keys.put(key.id(), key);
    }

    public void delete(String keyId) {
        keys.remove(keyId.toLowerCase());
    }

    public void giveVirtual(UUID uniqueId, String keyId, int amount) {
        PlayerProfile profile = playerDataManager.getOrLoad(uniqueId);
        profile.virtualKeys().merge(keyId.toLowerCase(), amount, Integer::sum);
        playerDataManager.save(profile);
    }

    public int getVirtual(UUID uniqueId, String keyId) {
        return playerDataManager.getOrLoad(uniqueId).virtualKeys().getOrDefault(keyId.toLowerCase(), 0);
    }

    public boolean consumeVirtual(UUID uniqueId, String keyId, int amount) {
        PlayerProfile profile = playerDataManager.getOrLoad(uniqueId);
        int current = profile.virtualKeys().getOrDefault(keyId.toLowerCase(), 0);
        if (current < amount) {
            return false;
        }
        profile.virtualKeys().put(keyId.toLowerCase(), current - amount);
        playerDataManager.save(profile);
        return true;
    }

    public ItemStack createPhysicalKey(String keyId, int amount) {
        CrateKey key = get(keyId).orElseThrow();
        ItemStack item = key.item().clone();
        item.setAmount(amount);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(BloodlineCratesPlugin.keyIdKey(), PersistentDataType.STRING, key.id());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isMatchingPhysicalKey(ItemStack itemStack, String keyId) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }
        String stored = itemStack.getItemMeta().getPersistentDataContainer().get(BloodlineCratesPlugin.keyIdKey(), PersistentDataType.STRING);
        return keyId.equalsIgnoreCase(stored);
    }

    public void grantTimedKeys() {
        long now = System.currentTimeMillis();
        Bukkit.getOnlinePlayers().forEach(player -> {
            PlayerProfile profile = playerDataManager.getOrLoad(player.getUniqueId());
            for (CrateKey key : keys.values()) {
                for (TimedKeyRule rule : key.timedKeyRules()) {
                    String token = key.id() + ":" + rule.intervalMinutes();
                    long next = profile.timedKeyNextClaim().getOrDefault(token, 0L);
                    if (next > now) {
                        continue;
                    }
                    int amount = (int) Math.max(1, Math.round(rule.amount() * resolveMultiplier(player, rule)));
                    giveVirtual(player.getUniqueId(), key.id(), amount);
                    profile.timedKeyNextClaim().put(token, now + (rule.intervalMinutes() * 60_000L));
                    playerDataManager.save(profile);
                }
            }
        });
    }

    public long nextTimedKeyMillis(UUID uniqueId, String keyId) {
        PlayerProfile profile = playerDataManager.getOrLoad(uniqueId);
        return profile.timedKeyNextClaim().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(keyId.toLowerCase() + ":"))
                .mapToLong(Map.Entry::getValue)
                .min()
                .orElse(0L);
    }

    private double resolveMultiplier(org.bukkit.entity.Player player, TimedKeyRule rule) {
        return rule.permissionMultiplier().entrySet().stream()
                .filter(entry -> player.hasPermission(entry.getKey()))
                .map(Map.Entry::getValue)
                .max(Double::compareTo)
                .orElse(1.0D);
    }

    private ItemStack buildItem(ConfigurationSection itemSection, String defaultName, java.util.List<String> defaultLore) {
        Material material = Material.TRIPWIRE_HOOK;
        int amount = 1;
        String name = defaultName;
        java.util.List<String> lore = defaultLore;
        if (itemSection != null) {
            material = Material.matchMaterial(itemSection.getString("material", "TRIPWIRE_HOOK"));
            amount = Math.max(1, itemSection.getInt("amount", 1));
            name = itemSection.getString("name", defaultName);
            lore = itemSection.getStringList("lore").isEmpty() ? defaultLore : itemSection.getStringList("lore");
        }
        ItemStack item = new ItemStack(material == null ? Material.TRIPWIRE_HOOK : material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.component(name));
        meta.lore(Text.color(lore).stream().map(Text::component).toList());
        item.setItemMeta(meta);
        return item;
    }
}
