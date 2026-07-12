package dev.linqfy.bigCasares.modules.nexus;

import java.util.Objects;
import java.util.UUID;

public record NexusPosition(
        UUID worldId,
        double x,
        double y,
        double z,
        float yaw
) {

    public NexusPosition {
        Objects.requireNonNull(worldId, "worldId");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yaw)) {
            throw new IllegalArgumentException("Nexus coordinates must be finite");
        }
    }

    public NexusBlockPosition blockPosition() {
        return new NexusBlockPosition(floor(x), floor(y), floor(z));
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }
}
