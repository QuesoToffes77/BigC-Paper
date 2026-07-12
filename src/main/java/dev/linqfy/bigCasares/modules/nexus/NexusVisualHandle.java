package dev.linqfy.bigCasares.modules.nexus;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record NexusVisualHandle(
        NexusId nexusId,
        UUID anchorEntityId,
        Set<UUID> childEntityIds
) {

    public NexusVisualHandle {
        Objects.requireNonNull(nexusId, "nexusId");
        Objects.requireNonNull(anchorEntityId, "anchorEntityId");
        childEntityIds = Set.copyOf(Objects.requireNonNull(childEntityIds, "childEntityIds"));
    }

    public boolean isComplete(int minimumChildren) {
        return minimumChildren >= 0 && childEntityIds.size() >= minimumChildren;
    }
}
