package dev.linqfy.bigCasares.modules.nexus;

public record NexusPlacementSettings(
        int containerClearanceHorizontal,
        int containerClearanceVertical,
        int minimumOpenFaces,
        int minimumWalkableWidth,
        boolean preventDoorwayPlacement
) {

    public NexusPlacementSettings {
        if (containerClearanceHorizontal < 0 || containerClearanceVertical < 0) {
            throw new IllegalArgumentException("Container clearances cannot be negative");
        }
        if (minimumOpenFaces < 0 || minimumOpenFaces > 4) {
            throw new IllegalArgumentException("minimumOpenFaces must be between zero and four");
        }
        if (minimumWalkableWidth < 1) {
            throw new IllegalArgumentException("minimumWalkableWidth must be positive");
        }
    }

    public static NexusPlacementSettings defaults() {
        return new NexusPlacementSettings(4, 3, 2, 2, true);
    }
}
