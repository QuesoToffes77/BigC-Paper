package dev.linqfy.bigCasares.modules.bounties;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface BountyStorage {

    Optional<BountyPlayerState> load(UUID playerId);

    void save(BountyPlayerState state);

    default Stream<BountyPlayerState> all() {
        return Stream.empty();
    }
}
