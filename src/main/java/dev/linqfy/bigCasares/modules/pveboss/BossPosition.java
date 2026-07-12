package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Objects;

public record BossPosition(double x, double y, double z) {

    public BossPosition {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("position coordinates must be finite");
        }
    }

    public double distanceSquared(BossPosition other) {
        Objects.requireNonNull(other, "other");
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
}
