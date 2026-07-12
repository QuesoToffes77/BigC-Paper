package dev.linqfy.bigCasares.modules.nexus;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class NexusVisualRecoveryPlanner {

    private final int minimumChildren;

    public NexusVisualRecoveryPlanner(int minimumChildren) {
        if (minimumChildren < 0) {
            throw new IllegalArgumentException("minimumChildren cannot be negative");
        }
        this.minimumChildren = minimumChildren;
    }

    public NexusVisualRecoveryPlan plan(
            Collection<NexusVisualRequest> desiredVisuals,
            Collection<NexusVisualHandle> activeVisuals
    ) {
        Objects.requireNonNull(desiredVisuals, "desiredVisuals");
        Objects.requireNonNull(activeVisuals, "activeVisuals");

        Map<NexusId, NexusVisualRequest> desiredById = new LinkedHashMap<>();
        for (NexusVisualRequest request : desiredVisuals) {
            desiredById.put(request.nexusId(), request);
        }

        Map<NexusId, NexusVisualHandle> activeById = new LinkedHashMap<>();
        for (NexusVisualHandle handle : activeVisuals) {
            activeById.put(handle.nexusId(), handle);
        }

        Set<NexusVisualRequest> toSpawn = new LinkedHashSet<>();
        Set<NexusId> toRemove = new LinkedHashSet<>();

        for (NexusVisualRequest desired : desiredById.values()) {
            NexusVisualHandle current = activeById.get(desired.nexusId());
            if (current == null) {
                toSpawn.add(desired);
            } else if (!current.isComplete(minimumChildren)) {
                toRemove.add(desired.nexusId());
                toSpawn.add(desired);
            }
        }

        for (NexusId activeId : activeById.keySet()) {
            if (!desiredById.containsKey(activeId)) {
                toRemove.add(activeId);
            }
        }

        return new NexusVisualRecoveryPlan(toSpawn, toRemove);
    }
}
