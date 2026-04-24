package com.bloodline.crates.placement;

import com.bloodline.crates.BloodlineCrates;
import com.bloodline.crates.model.Crate;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;

import java.util.*;

public class PlacementManager {
    private final BloodlineCrates plugin;
    private final PlacedCrateStore store;
    private final HologramHandler hologramHandler;
    private final Map<String, PlacedCrate> placementsByLocation = new HashMap<>();
    private final Map<UUID, Integer> opensToday = new HashMap<>();

    public PlacementManager(BloodlineCrates plugin, PlacedCrateStore store, HologramHandler hologramHandler) {
        this.plugin = plugin;
        this.store = store;
        this.hologramHandler = hologramHandler;
        loadPlacements();
    }

    public PlacedCrate placeCrate(Player placer, String crateId, Location location) {
        String key = locationKey(location);
        if (placementsByLocation.containsKey(key)) {
            throw new IllegalStateException("A crate is already placed there.");
        }

        Crate crate = plugin.getConfigManager().getCrate(crateId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown crate: " + crateId));

        location.getBlock().setType(Material.CHEST, false);
        BlockFace facing = placer == null ? BlockFace.NORTH : placer.getFacing().getOppositeFace();
        applyChestFacing(location, facing);
        PlacedCrate placedCrate = new PlacedCrate(
            UUID.randomUUID(),
            crateId,
            location.getBlock().getLocation(),
            null,
            placer == null ? null : placer.getUniqueId(),
            System.currentTimeMillis(),
            facing
        );
        placementsByLocation.put(key, placedCrate);
        refreshHologram(placedCrate, crate);
        save();
        return placedCrate;
    }

    public Optional<PlacedCrate> removeCrate(Location location) {
        PlacedCrate removed = placementsByLocation.remove(locationKey(location));
        if (removed != null) {
            hologramHandler.deleteHologram(removed.getHologramId());
            Material currentType = location.getBlock().getType();
            if (currentType == Material.CHEST || currentType == Material.BARREL || currentType == Material.TRAPPED_CHEST) {
                location.getBlock().setType(Material.AIR, false);
            }
            save();
        }
        return Optional.ofNullable(removed);
    }

    public Optional<PlacedCrate> getCrateAt(Location location) {
        return Optional.ofNullable(placementsByLocation.get(locationKey(location)));
    }

    public Collection<PlacedCrate> getAllPlacements() {
        return Collections.unmodifiableCollection(placementsByLocation.values());
    }

    public void recordOpen(Location location) {
        getCrateAt(location).ifPresent(placedCrate -> {
            opensToday.merge(placedCrate.getId(), 1, Integer::sum);
            plugin.getConfigManager().getCrate(placedCrate.getCrateId()).ifPresent(crate -> refreshHologram(placedCrate, crate));
        });
    }

    public void shutdown() {
        for (PlacedCrate placement : placementsByLocation.values()) {
            hologramHandler.deleteHologram(placement.getHologramId());
        }
        save();
    }

    public void refreshPlacements() {
        for (PlacedCrate placement : placementsByLocation.values()) {
            hologramHandler.deleteHologram(placement.getHologramId());
            placement.setHologramId(null);
            plugin.getConfigManager().getCrate(placement.getCrateId()).ifPresent(crate -> {
                placement.getLocation().getBlock().setType(Material.CHEST, false);
                applyChestFacing(placement.getLocation(), placement.getFacing());
                refreshHologram(placement, crate);
            });
        }
        save();
    }

    private void loadPlacements() {
        placementsByLocation.clear();
        for (PlacedCrate placedCrate : store.load()) {
            placementsByLocation.put(locationKey(placedCrate.getLocation()), placedCrate);
            plugin.getConfigManager().getCrate(placedCrate.getCrateId()).ifPresent(crate -> {
                placedCrate.getLocation().getBlock().setType(Material.CHEST, false);
                applyChestFacing(placedCrate.getLocation(), placedCrate.getFacing());
                refreshHologram(placedCrate, crate);
            });
        }
        plugin.getLogger().info("Loaded " + placementsByLocation.size() + " placed crate(s)");
    }

    private void save() {
        store.save(placementsByLocation.values());
    }

    private void refreshHologram(PlacedCrate placedCrate, Crate crate) {
        if (!crate.isHologramEnabled()) {
            if (placedCrate.getHologramId() != null) {
                hologramHandler.deleteHologram(placedCrate.getHologramId());
                placedCrate.setHologramId(null);
            }
            return;
        }

        List<String> lines = resolveHologramLines(placedCrate, crate);
        Location hologramLocation = placedCrate.getLocation().clone().add(0.5D, 1.8D, 0.5D);
        if (placedCrate.getHologramId() == null) {
            String hologramId = hologramHandler.createHologram(hologramHandler.generateName(), hologramLocation, lines);
            placedCrate.setHologramId(hologramId);
        } else {
            hologramHandler.updateHologram(placedCrate.getHologramId(), lines);
        }
    }

    private List<String> resolveHologramLines(PlacedCrate placedCrate, Crate crate) {
        if (plugin.getLimitManager() != null && plugin.getLimitManager().isSoldOut(crate)) {
            return List.of(ChatColor.translateAlternateColorCodes('&', "&c&lSOLD OUT"));
        }

        String keyName = crate.getKeyDisplayItem() != null && crate.getKeyDisplayItem().hasItemMeta() && crate.getKeyDisplayItem().getItemMeta().hasDisplayName()
            ? crate.getKeyDisplayItem().getItemMeta().getDisplayName()
            : crate.getKeyId();
        int opens = opensToday.getOrDefault(placedCrate.getId(), 0);

        List<String> lines = new ArrayList<>();
        for (String line : crate.getHologramLines()) {
            String resolved = line
                .replace("{crate_name}", crate.getDisplayName())
                .replace("{key_name}", keyName == null ? crate.getKeyId() : keyName)
                .replace("{opens_today}", String.valueOf(opens));
            lines.add(ChatColor.translateAlternateColorCodes('&', resolved));
        }
        return lines;
    }

    private String locationKey(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private void applyChestFacing(Location location, BlockFace facing) {
        BlockData blockData = location.getBlock().getBlockData();
        if (blockData instanceof Directional directional) {
            BlockFace resolved = facing == null ? BlockFace.NORTH : facing;
            if (!directional.getFaces().contains(resolved)) {
                resolved = BlockFace.NORTH;
            }
            directional.setFacing(resolved);
            location.getBlock().setBlockData(directional, false);
        }
    }
}
