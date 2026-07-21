package dev.linqfy.bigCasares.modules.pveboss;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record BossDamageContribution(UUID playerId, double damage, Instant reachedAt) {
    public BossDamageContribution {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(reachedAt, "reachedAt");
    }
}
