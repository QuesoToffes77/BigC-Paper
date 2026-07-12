package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Objects;
import java.util.UUID;

public record BossView(
    UUID bossInstanceId,
    String displayName,
    BossHealthView health,
    BossPhaseView phase,
    String specialState
) {

    public BossView {
        Objects.requireNonNull(bossInstanceId, "bossInstanceId");
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("boss displayName must not be blank");
        }
        Objects.requireNonNull(health, "health");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(specialState, "specialState");
        if (!bossInstanceId.equals(health.bossInstanceId()) || !bossInstanceId.equals(phase.bossInstanceId())) {
            throw new IllegalArgumentException("boss view children must belong to the same boss instance");
        }
    }

    public BossView withHealth(BossHealthView nextHealth) {
        return new BossView(bossInstanceId, displayName, nextHealth, phase, specialState);
    }

    public BossView withPhase(BossPhaseView nextPhase) {
        return new BossView(bossInstanceId, displayName, health, nextPhase, nextPhase.specialState());
    }
}
