package com.bloodline.crates.manager;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.debug.DebugEventType;
import com.bloodline.crates.model.Crate;
import com.bloodline.crates.model.CrateKey;
import com.bloodline.crates.model.KeyMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class KeyManager {
    private final BloodlineCrates plugin;
    private final NamespacedKey crateKeyKey;

    public KeyManager(BloodlineCrates plugin) {
        this.plugin = plugin;
        this.crateKeyKey = new NamespacedKey(plugin, "crate_key");
    }

    public void giveKey(Player player, CrateKey crateKey, int amount) {
        ItemStack keyItem = crateKey.getDisplayItem().clone();
        keyItem.setAmount(amount);

        ItemMeta meta = keyItem.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(crateKeyKey, PersistentDataType.STRING, crateKey.getCrateId());
            keyItem.setItemMeta(meta);
        }

        player.getInventory().addItem(keyItem);
    }

    public void giveVirtualKey(Player player, String crateId, int amount) {
        if (player == null || crateId == null || crateId.isBlank() || amount <= 0) {
            return;
        }
        Map<String, Integer> keys = new LinkedHashMap<>(plugin.getPlayerDataManager().getVirtualKeys(player.getUniqueId()));
        String normalizedId = crateId.toLowerCase(Locale.ROOT);
        keys.put(normalizedId, keys.getOrDefault(normalizedId, 0) + amount);
        plugin.getPlayerDataManager().setVirtualKeys(player.getUniqueId(), keys);
    }

    public boolean hasKey(Player player, Crate crate) {
        int keyCount = getKeyCount(player, crate);
        boolean hasKey = keyCount > 0;
        plugin.getDebugManager().emit(DebugEventType.KEY_CHECK, player.getUniqueId(),
            () -> "crate=" + crate.getId() + ", mode=" + crate.getKeyMode() + ", count=" + keyCount + ", allowed=" + hasKey);
        return hasKey;
    }

    public int getKeyCount(Player player, Crate crate) {
        if (crate == null) {
            return 0;
        }
        return crate.getKeyMode() == KeyMode.VIRTUAL
            ? getVirtualKeyCount(player, crate.getId())
            : getPhysicalKeyCount(player, crate.getId());
    }

    public int getPhysicalKeyCount(Player player, String crateId) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            String keyId = meta.getPersistentDataContainer().get(crateKeyKey, PersistentDataType.STRING);
            if (crateId.equals(keyId)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public int getVirtualKeyCount(Player player, String crateId) {
        if (player == null || crateId == null || crateId.isBlank()) {
            return 0;
        }
        return plugin.getPlayerDataManager().getVirtualKeys(player.getUniqueId())
            .getOrDefault(crateId.toLowerCase(Locale.ROOT), 0);
    }

    public boolean consumeKey(Player player, Crate crate) {
        if (crate == null) {
            return false;
        }
        return crate.getKeyMode() == KeyMode.VIRTUAL
            ? consumeVirtualKey(player, crate.getId())
            : consumePhysicalKey(player, crate.getId());
    }

    private boolean consumePhysicalKey(Player player, String crateId) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            String keyId = meta.getPersistentDataContainer().get(crateKeyKey, PersistentDataType.STRING);
            if (crateId.equals(keyId)) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().remove(item);
                }
                return true;
            }
        }
        return false;
    }

    public boolean consumeVirtualKey(Player player, String crateId) {
        if (player == null || crateId == null || crateId.isBlank()) {
            return false;
        }

        Map<String, Integer> keys = new LinkedHashMap<>(plugin.getPlayerDataManager().getVirtualKeys(player.getUniqueId()));
        String normalizedId = crateId.toLowerCase(Locale.ROOT);
        int current = keys.getOrDefault(normalizedId, 0);
        if (current <= 0) {
            return false;
        }

        if (current == 1) {
            keys.remove(normalizedId);
        } else {
            keys.put(normalizedId, current - 1);
        }
        plugin.getPlayerDataManager().setVirtualKeys(player.getUniqueId(), keys);
        return true;
    }

    public NamespacedKey getCrateKeyKey() {
        return crateKeyKey;
    }

    public Collection<CrateKey> getAllKeys() {
        return plugin.getConfigManager().getAllCrates().stream()
            .map(crate -> new CrateKey(crate.getId(), crate.getKeyDisplayItem() == null ? null : crate.getKeyDisplayItem().clone()))
            .toList();
    }

    public void saveKey(CrateKey crateKey) {
        if (crateKey == null || crateKey.getCrateId() == null || crateKey.getCrateId().isBlank()) {
            return;
        }

        plugin.getConfigManager().getCrate(crateKey.getCrateId()).ifPresent(crate -> {
            Crate updatedCrate = new Crate(
                crate.getId(),
                crate.getDisplayName(),
                crate.getType(),
                crate.getKeyId(),
                List.copyOf(crate.getRewards()),
                crate.getAnimationConfig(),
                crate.isPityEnabled(),
                crate.getPityThreshold(),
                crate.getJackpotRewardId(),
                crate.getDescription(),
                crateKey.getDisplayItem() == null ? null : crateKey.getDisplayItem().clone(),
                crate.getGuiRows(),
                crate.getRewardSlots()
            );
            updatedCrate.setHologramEnabled(crate.isHologramEnabled());
            updatedCrate.setHologramLines(crate.getHologramLines());
            updatedCrate.setCooldownEnabled(crate.isCooldownEnabled());
            updatedCrate.setCooldownDurationSeconds(crate.getCooldownDurationSeconds());
            updatedCrate.setCooldownBypassPermission(crate.getCooldownBypassPermission());
            updatedCrate.setCooldownMessage(crate.getCooldownMessage());
            updatedCrate.setKeyMode(crate.getKeyMode());
            updatedCrate.setVirtualOnly(crate.isVirtualOnly());
            plugin.getConfigManager().saveCrate(updatedCrate);
        });
    }
}
