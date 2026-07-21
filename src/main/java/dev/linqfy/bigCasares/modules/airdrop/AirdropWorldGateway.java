package dev.linqfy.bigCasares.modules.airdrop;

import java.util.Optional;

public interface AirdropWorldGateway {
    int getHighestBlockY(int x, int z);

    boolean isSafeLandingBlock(int x, int y, int z);

    boolean isSafeOpenSpace(int x, int y, int z);

    Optional<AirdropPosition> getRandomPlayerPosition();
}
