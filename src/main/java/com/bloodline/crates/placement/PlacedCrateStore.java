package com.bloodline.crates.placement;

import com.bloodline.crates.BloodlineCrates;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class PlacedCrateStore {
    private final BloodlineCrates plugin;
    private final File file;

    public PlacedCrateStore(BloodlineCrates plugin) {
        this.plugin = plugin;
        File folder = new File(plugin.getDataFolder(), "placements");
        folder.mkdirs();
        this.file = new File(folder, "placements.yml");
    }

    public List<PlacedCrate> load() {
        List<PlacedCrate> placements = new ArrayList<>();
        if (!file.exists()) {
            return placements;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<?> list = config.getList("placements");
        if (list == null) {
            return placements;
        }

        for (Object entry : list) {
            if (!(entry instanceof java.util.Map<?, ?> rawMap)) {
                continue;
            }

            String worldName = String.valueOf(rawMap.get("world"));
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Skipping placed crate because world is missing: " + worldName);
                continue;
            }

            try {
                UUID id = UUID.fromString(String.valueOf(rawMap.get("id")));
                String crateId = String.valueOf(rawMap.get("crate-id"));
                double x = doubleValue(rawMap.get("x"));
                double y = doubleValue(rawMap.get("y"));
                double z = doubleValue(rawMap.get("z"));
                float yaw = (float) doubleValue(rawMap.get("yaw"));
                float pitch = (float) doubleValue(rawMap.get("pitch"));
                String hologramId = rawMap.get("hologram-id") == null ? null : String.valueOf(rawMap.get("hologram-id"));
                UUID placedBy = rawMap.get("placed-by") == null ? null : UUID.fromString(String.valueOf(rawMap.get("placed-by")));
                long placedAt = longValue(rawMap.get("placed-at"));
                BlockFace facing = rawMap.get("facing") == null
                    ? BlockFace.NORTH
                    : BlockFace.valueOf(String.valueOf(rawMap.get("facing")).toUpperCase());

                placements.add(new PlacedCrate(
                    id,
                    crateId,
                    new Location(world, x, y, z, yaw, pitch),
                    hologramId,
                    placedBy,
                    placedAt,
                    facing
                ));
            } catch (Exception exception) {
                plugin.getLogger().warning("Failed to load placed crate entry: " + exception.getMessage());
            }
        }

        return placements;
    }

    public void save(Collection<PlacedCrate> placements) {
        YamlConfiguration config = new YamlConfiguration();
        List<java.util.Map<String, Object>> serialized = new ArrayList<>();
        for (PlacedCrate placement : placements) {
            Location location = placement.getLocation();
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", placement.getId().toString());
            map.put("crate-id", placement.getCrateId());
            map.put("world", location.getWorld().getName());
            map.put("x", location.getBlockX());
            map.put("y", location.getBlockY());
            map.put("z", location.getBlockZ());
            map.put("yaw", location.getYaw());
            map.put("pitch", location.getPitch());
            map.put("hologram-id", placement.getHologramId());
            map.put("placed-by", placement.getPlacedByUUID() == null ? null : placement.getPlacedByUUID().toString());
            map.put("placed-at", placement.getPlacedAtTimestamp());
            map.put("facing", placement.getFacing() == null ? BlockFace.NORTH.name() : placement.getFacing().name());
            serialized.add(map);
        }
        config.set("placements", serialized);

        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save placements: " + exception.getMessage());
        }
    }

    private double doubleValue(Object value) {
        return value instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(value));
    }

    private long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }
}
