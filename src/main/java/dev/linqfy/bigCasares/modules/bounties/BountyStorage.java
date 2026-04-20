package dev.linqfy.bigCasares.modules.bounties;

import java.util.Optional;
import java.util.UUID;

public interface BountyStorage {

    Optional<BountyPlayerState> load(UUID playerId);

    void save(BountyPlayerState state);
}
