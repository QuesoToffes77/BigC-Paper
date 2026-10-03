package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public final class BukkitAirdropWorldGateway implements AirdropWorldGateway {

    private static final Set<Material> INVALID_GROUNDS = Set.of(
            Material.WATER,
            Material.LAVA,
            Material.VOID_AIR
    );

    private final World world;

    public BukkitAirdropWorldGateway(World world) {
        this.world = world;
    }

    @Override
    public int getHighestBlockY(int x, int z) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return world.getMinHeight();
        }
        int y = world.getHighestBlockYAt(x, z);
        while (y > world.getMinHeight()) {
            Material type = world.getBlockAt(x, y, z).getType();
            if (type.isSolid() && !INVALID_GROUNDS.contains(type)) {
                return y;
            }
            y--;
        }
        return world.getHighestBlockYAt(x, z);
    }

    @Override
    public boolean isSafeLandingBlock(int x, int y, int z) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return false;
        }
        Material material = world.getBlockAt(x, y, z).getType();
        if (INVALID_GROUNDS.contains(material)) {
            return false;
        }
        return material.isSolid();
    }

    @Override
    public boolean isSafeOpenSpace(int x, int y, int z) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return false;
        }
        Block block = world.getBlockAt(x, y, z);
        Material material = block.getType();
        if (material == Material.LAVA || material == Material.WATER) {
            return false;
        }
        return block.isPassable();
    }

    @Override
    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        return world.isChunkLoaded(chunkX, chunkZ);
    }

    @Override
    public Optional<AirdropPosition> getRandomPlayerPosition() {
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) {
            return Optional.empty();
        }
        Player randomPlayer = players.get(new Random().nextInt(players.size()));
        return Optional.of(new AirdropPosition(
                randomPlayer.getLocation().getBlockX(),
                randomPlayer.getLocation().getBlockY(),
                randomPlayer.getLocation().getBlockZ()
        ));
    }
}
