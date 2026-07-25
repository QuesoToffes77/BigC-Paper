package dev.linqfy.bigCasares.modules.endevent;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;

import java.util.Set;

public final class EndEventSafeLocations {
    private static final Set<Material> DANGEROUS = Set.of(
        Material.MAGMA_BLOCK,
        Material.CACTUS,
        Material.CAMPFIRE,
        Material.SOUL_CAMPFIRE,
        Material.FIRE,
        Material.SOUL_FIRE,
        Material.LAVA,
        Material.POWDER_SNOW,
        Material.SWEET_BERRY_BUSH
    );

    public Location insideBorder(World world, Location desired, double margin) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        EndEventBorderPoint projected = EndEventBorderSafety.projectInside(
            center.getX(), center.getZ(), border.getSize(), margin, desired.getX(), desired.getZ()
        );
        Location safe = search(world, projected.x(), projected.z(), 8, margin);
        if (safe != null) {
            return safe;
        }
        EndEventBorderPoint centerPoint = EndEventBorderSafety.projectInside(
            center.getX(), center.getZ(), border.getSize(), margin, center.getX(), center.getZ()
        );
        safe = search(world, centerPoint.x(), centerPoint.z(), 16, margin);
        return safe == null ? emergencyPlatform(world, centerPoint.x(), centerPoint.z()) : safe;
    }

    public boolean isInsideInset(World world, Location location, double margin) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        return EndEventBorderSafety.isInsideInset(
            center.getX(), center.getZ(), border.getSize(), margin, location.getX(), location.getZ()
        );
    }

    private Location search(World world, double requestedX, double requestedZ, int radius, double margin) {
        int originX = (int) Math.floor(requestedX);
        int originZ = (int) Math.floor(requestedZ);
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (ring > 0 && Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        continue;
                    }
                    int x = originX + dx;
                    int z = originZ + dz;
                    WorldBorder border = world.getWorldBorder();
                    Location center = border.getCenter();
                    if (!EndEventBorderSafety.isInsideInset(
                        center.getX(), center.getZ(), border.getSize(), margin, x + 0.5, z + 0.5
                    )) {
                        continue;
                    }
                    int y = world.getHighestBlockYAt(x, z) + 1;
                    if (isSafe(world, x, y, z)) {
                        return new Location(world, x + 0.5, y, z + 0.5);
                    }
                }
            }
        }
        return null;
    }

    private boolean isSafe(World world, int x, int y, int z) {
        if (y <= world.getMinHeight() || y + 1 >= world.getMaxHeight()) {
            return false;
        }
        Material floor = world.getBlockAt(x, y - 1, z).getType();
        return floor.isSolid()
            && !DANGEROUS.contains(floor)
            && world.getBlockAt(x, y, z).isPassable()
            && world.getBlockAt(x, y + 1, z).isPassable();
    }

    private Location emergencyPlatform(World world, double xValue, double zValue) {
        int x = (int) Math.floor(xValue);
        int z = (int) Math.floor(zValue);
        int y = Math.min(
            world.getMaxHeight() - 3,
            Math.max(world.getSeaLevel() + 2, world.getHighestBlockYAt(x, z) + 2)
        );
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(x + dx, y - 1, z + dz).setType(Material.OBSIDIAN, false);
                world.getBlockAt(x + dx, y, z + dz).setType(Material.AIR, false);
                world.getBlockAt(x + dx, y + 1, z + dz).setType(Material.AIR, false);
            }
        }
        return new Location(world, x + 0.5, y, z + 0.5);
    }
}
