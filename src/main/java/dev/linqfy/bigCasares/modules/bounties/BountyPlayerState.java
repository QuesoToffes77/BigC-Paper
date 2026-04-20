package dev.linqfy.bigCasares.modules.bounties;

import java.time.Instant;
import java.util.UUID;

public record BountyPlayerState(
    UUID playerId,
    double activeBounty,
    Instant updatedAt
) {
    public static BountyPlayerState empty(UUID playerId, Instant updatedAt) {
        return new BountyPlayerState(playerId, 0.0, updatedAt);
    }
}
