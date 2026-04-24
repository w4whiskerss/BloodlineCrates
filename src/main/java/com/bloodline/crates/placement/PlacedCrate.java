package com.bloodline.crates.placement;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.UUID;

@Data
@AllArgsConstructor
public class PlacedCrate {
    private UUID id;
    private String crateId;
    private Location location;
    private String hologramId;
    private UUID placedByUUID;
    private long placedAtTimestamp;
    private BlockFace facing;
}
