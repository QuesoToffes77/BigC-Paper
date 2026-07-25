package dev.linqfy.bigCasares.modules.endevent;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.Structure;
import org.bukkit.generator.structure.StructurePiece;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.StructureSearchResult;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class EndEventPortalResolver {

    public Optional<Location> resolve(World world, int searchRadiusChunks) {
        StructureSearchResult located = world.locateNearestStructure(
            world.getSpawnLocation(), Structure.STRONGHOLD, searchRadiusChunks, false
        );
        if (located == null) {
            return Optional.empty();
        }
        Location origin = located.getLocation();
        int originChunkX = origin.getBlockX() >> 4;
        int originChunkZ = origin.getBlockZ() >> 4;
        Set<GeneratedStructure> inspected = new HashSet<>();
        for (int radius = 0; radius <= 10; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    int chunkX = originChunkX + dx;
                    int chunkZ = originChunkZ + dz;
                    world.loadChunk(chunkX, chunkZ);
                    for (GeneratedStructure generated : world.getStructures(chunkX, chunkZ, Structure.STRONGHOLD)) {
                        if (inspected.add(generated)) {
                            Optional<Location> portal = scanPieces(world, generated);
                            if (portal.isPresent()) {
                                return portal;
                            }
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Location> scanPieces(World world, GeneratedStructure structure) {
        for (StructurePiece piece : structure.getPieces()) {
            Optional<Location> portal = scanBox(world, piece.getBoundingBox());
            if (portal.isPresent()) {
                return portal;
            }
        }
        return scanBox(world, structure.getBoundingBox());
    }

    private Optional<Location> scanBox(World world, BoundingBox box) {
        long volume = (long) Math.ceil(box.getWidthX())
            * (long) Math.ceil(box.getHeight())
            * (long) Math.ceil(box.getWidthZ());
        if (volume > 2_000_000L) {
            return Optional.empty();
        }
        int count = 0;
        double sumX = 0;
        double sumY = 0;
        double sumZ = 0;
        for (int x = (int) Math.floor(box.getMinX()); x <= (int) Math.ceil(box.getMaxX()); x++) {
            for (int y = (int) Math.floor(box.getMinY()); y <= (int) Math.ceil(box.getMaxY()); y++) {
                for (int z = (int) Math.floor(box.getMinZ()); z <= (int) Math.ceil(box.getMaxZ()); z++) {
                    if (world.getBlockAt(x, y, z).getType() == Material.END_PORTAL_FRAME) {
                        count++;
                        sumX += x + 0.5;
                        sumY += y;
                        sumZ += z + 0.5;
                    }
                }
            }
        }
        if (count < 12) {
            return Optional.empty();
        }
        return Optional.of(new Location(world, sumX / count, sumY / count + 1.0, sumZ / count));
    }
}
