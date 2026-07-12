package dev.linqfy.bigCasares.modules.nexus;

import java.util.Objects;

public record NexusPlacementAttempt(NexusId nexusId, NexusPlacementResult result) {
    public NexusPlacementAttempt {
        Objects.requireNonNull(nexusId, "nexusId");
        Objects.requireNonNull(result, "result");
    }
}
