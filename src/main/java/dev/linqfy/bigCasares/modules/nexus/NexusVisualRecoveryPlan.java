package dev.linqfy.bigCasares.modules.nexus;

import java.util.Objects;
import java.util.Set;

public record NexusVisualRecoveryPlan(
        Set<NexusVisualRequest> toSpawn,
        Set<NexusId> toRemove
) {

    public NexusVisualRecoveryPlan {
        toSpawn = Set.copyOf(Objects.requireNonNull(toSpawn, "toSpawn"));
        toRemove = Set.copyOf(Objects.requireNonNull(toRemove, "toRemove"));
    }
}
