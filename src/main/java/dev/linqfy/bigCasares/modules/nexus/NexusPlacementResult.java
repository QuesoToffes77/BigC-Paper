package dev.linqfy.bigCasares.modules.nexus;

import java.util.Objects;
import java.util.Optional;

public record NexusPlacementResult(
        NexusPlacementRejection rejection,
        Optional<NexusBlockPosition> blockingPosition,
        Optional<String> blockingMaterial
) {

    public NexusPlacementResult {
        Objects.requireNonNull(rejection, "rejection");
        blockingPosition = Objects.requireNonNull(blockingPosition, "blockingPosition");
        blockingMaterial = Objects.requireNonNull(blockingMaterial, "blockingMaterial");
    }

    public static NexusPlacementResult allowedResult() {
        return new NexusPlacementResult(NexusPlacementRejection.NONE, Optional.empty(), Optional.empty());
    }

    public static NexusPlacementResult rejected(
            NexusPlacementRejection rejection,
            NexusBlockPosition position,
            String material
    ) {
        if (rejection == NexusPlacementRejection.NONE) {
            throw new IllegalArgumentException("A rejected result needs a rejection reason");
        }
        return new NexusPlacementResult(
                rejection,
                Optional.ofNullable(position),
                Optional.ofNullable(material)
        );
    }

    public boolean allowed() {
        return rejection == NexusPlacementRejection.NONE;
    }
}
