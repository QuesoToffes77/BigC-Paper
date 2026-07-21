package dev.linqfy.bigCasares.modules.tombstone;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;

public record TombstoneRecord(
    UUID id,
    UUID ownerId,
    String ownerName,
    UUID worldId,
    double x,
    double y,
    double z,
    float yaw,
    Instant expiresAt,
    List<ItemStack> items
) {
    public TombstoneRecord {
        List<ItemStack> copied = new ArrayList<>(items.size());
        items.forEach(item -> copied.add(TombstoneItemLayout.cloneOrNull(item)));
        items = Collections.unmodifiableList(copied);
    }

    public Location location(org.bukkit.World world) {
        return new Location(world, x, y, z, yaw, 0.0f);
    }
}
