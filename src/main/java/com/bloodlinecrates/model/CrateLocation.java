package com.bloodlinecrates.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

public record CrateLocation(String world, double x, double y, double z, float yaw, float pitch) {

    public static CrateLocation fromLocation(Location location) {
        return new CrateLocation(
                Objects.requireNonNull(location.getWorld()).getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public static CrateLocation fromConfig(ConfigurationSection section) {
        return new CrateLocation(
                section.getString("world", "world"),
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch")
        );
    }

    public Location toBukkitLocation() {
        World worldObject = Bukkit.getWorld(world);
        if (worldObject == null) {
            return null;
        }
        return new Location(worldObject, x, y, z, yaw, pitch);
    }
}
