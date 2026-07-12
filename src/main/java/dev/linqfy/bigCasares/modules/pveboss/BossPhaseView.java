package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Objects;
import java.util.UUID;

public record BossPhaseView(
    UUID bossInstanceId,
    int phase,
    String displayName,
    String specialState
) {

    public BossPhaseView {
        Objects.requireNonNull(bossInstanceId, "bossInstanceId");
        if (phase < 1) {
            throw new IllegalArgumentException("phase must be positive");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("phase displayName must not be blank");
        }
        Objects.requireNonNull(specialState, "specialState");
    }
}
