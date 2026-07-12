package dev.linqfy.bigCasares.modules.pveboss;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record BossAbilityRuntime(
    String abilityId,
    BossAbilityState state,
    Instant startedAt,
    Instant completesAt,
    Set<UUID> targets
) {

    public BossAbilityRuntime {
        if (abilityId == null || abilityId.isBlank()) {
            throw new IllegalArgumentException("abilityId must not be blank");
        }
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(completesAt, "completesAt");
        Objects.requireNonNull(targets, "targets");
        if (completesAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("completesAt must not be before startedAt");
        }
        targets = Set.copyOf(targets);
    }

    public BossAbilityRuntime withState(BossAbilityState nextState) {
        return new BossAbilityRuntime(abilityId, nextState, startedAt, completesAt, targets);
    }
}
