package dev.linqfy.bigCasares.modules.nexus;

import java.util.Objects;
import java.util.Optional;

public record NexusSnapshot(
        NexusId nexusId,
        double currentHealth,
        double maximumHealth,
        Optional<NexusPosition> position
) {

    public NexusSnapshot {
        Objects.requireNonNull(nexusId, "nexusId");
        position = Objects.requireNonNull(position, "position");
    }

    public boolean destroyed() {
        return currentHealth <= 0.0;
    }
}
