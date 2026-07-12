package dev.linqfy.bigCasares.modules.nexus;

import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;

public final class BukkitNexusPlacementProbe implements NexusPlacementProbe {

    private final World world;

    public BukkitNexusPlacementProbe(World world) {
        this.world = Objects.requireNonNull(world, "world");
    }

    @Override
    public String materialAt(NexusBlockPosition position) {
        return blockAt(position).getType().name();
    }

    @Override
    public boolean isPassable(NexusBlockPosition position) {
        return blockAt(position).isPassable();
    }

    private Block blockAt(NexusBlockPosition position) {
        return world.getBlockAt(position.x(), position.y(), position.z());
    }
}
