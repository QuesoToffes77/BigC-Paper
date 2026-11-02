package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Zonas donde Don Pollos no toca el terreno ni deja poner sus bloques. En BigCasares son los volumenes protegidos de
 * los nexus (ver DonPollosModule); en los tests se reemplaza por una regla propia.
 */
@FunctionalInterface
interface DonPollosProtection {

    DonPollosProtection NONE = (world, minX, maxX, minY, maxY, minZ, maxZ) -> false;

    /** Si la caja [minX..maxX] x [minY..maxY] x [minZ..maxZ] toca una zona protegida. */
    boolean isProtected(World world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ);

    default boolean isProtected(Block block) {
        return isProtected(block.getWorld(), block.getX(), block.getX(), block.getY(), block.getY(), block.getZ(),
            block.getZ());
    }
}
