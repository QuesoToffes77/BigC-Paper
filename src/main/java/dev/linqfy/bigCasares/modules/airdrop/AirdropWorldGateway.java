package dev.linqfy.bigCasares.modules.airdrop;

import java.util.Optional;

public interface AirdropWorldGateway {
    int getHighestBlockY(int x, int z);

    boolean isSafeLandingBlock(int x, int y, int z);

    boolean isSafeOpenSpace(int x, int y, int z);

    Optional<AirdropPosition> getRandomPlayerPosition();

    /**
     * Checks if the chunk at the given chunk coordinates is loaded.
     * Default returns {@code true} for backward compatibility and testing.
     *
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return {@code true} if the chunk is loaded, {@code false} otherwise
     */
    default boolean isChunkLoaded(int chunkX, int chunkZ) {
        return true;
    }
}
