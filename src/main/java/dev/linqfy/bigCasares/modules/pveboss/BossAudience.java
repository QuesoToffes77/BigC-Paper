package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record BossAudience(Set<UUID> playerIds) {

    public BossAudience {
        Objects.requireNonNull(playerIds, "playerIds");
        playerIds = Set.copyOf(playerIds);
    }
}
