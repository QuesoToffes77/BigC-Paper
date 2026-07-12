package dev.linqfy.bigCasares.modules.nexus;

import java.util.Objects;

public record NexusDamageResult(
        NexusDamageOutcome outcome,
        double actualDamage,
        double multiplier,
        double appliedDamage,
        double previousHealth,
        double currentHealth
) {

    public NexusDamageResult {
        Objects.requireNonNull(outcome, "outcome");
    }
}
