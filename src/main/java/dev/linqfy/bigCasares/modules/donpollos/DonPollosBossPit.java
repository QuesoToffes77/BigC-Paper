package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.util.List;

/** El cubo vaciado: guarda como estaba cada bloque que toco (con su contenido) para dejarlo igual despues. */
record DonPollosBossPit(World world, int chunkX, int chunkZ, int feet, int bottom, List<BlockState> changed) {
    boolean contains(Location location) {
        return location.getWorld() == world && DonPollosCubeEscape.inside(chunkX, chunkZ, feet, bottom,
            location.getBlockX(), location.getBlockZ(), location.getY());
    }

    /** El cubo con sus paredes y su piso (el anillo de afuera del chunk incluido). */
    boolean containsBlock(Block block) {
        int x = block.getX() - chunkX * 16;
        int z = block.getZ() - chunkZ * 16;
        return block.getWorld() == world && x >= -1 && x <= 16 && z >= -1 && z <= 16
            && block.getY() >= bottom - 1 && block.getY() <= feet;
    }
}
