package dev.linqfy.bigCasares.modules.pveboss;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public record BossAbilityCastView(
    UUID bossInstanceId,
    String abilityId,
    String displayName,
    Duration remaining,
    BossTelegraphDefinition telegraph
) {

    public BossAbilityCastView {
        Objects.requireNonNull(bossInstanceId, "bossInstanceId");
        if (abilityId == null || abilityId.isBlank()) {
            throw new IllegalArgumentException("abilityId must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("ability displayName must not be blank");
        }
        Objects.requireNonNull(remaining, "remaining");
        Objects.requireNonNull(telegraph, "telegraph");
        if (remaining.isNegative()) {
            throw new IllegalArgumentException("remaining must not be negative");
        }
    }
}
