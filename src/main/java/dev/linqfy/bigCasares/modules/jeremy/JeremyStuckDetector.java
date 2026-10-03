package dev.linqfy.bigCasares.modules.jeremy;

final class JeremyStuckDetector {
    private final long stuckWindowTicks;
    private final double minimumProgress;
    private final double farDistance;
    private JeremyPosition anchorPosition;
    private double anchorDistance;
    private long lastProgressTick;
    private boolean stuck;

    JeremyStuckDetector(long stuckWindowTicks, double minimumProgress, double farDistance) {
        this.stuckWindowTicks = Math.max(1L, stuckWindowTicks);
        this.minimumProgress = Math.max(0.01, minimumProgress);
        this.farDistance = Math.max(0.0, farDistance);
    }

    boolean sample(long currentTick, JeremyPosition position, double distanceToTarget, boolean pursuing) {
        if (!pursuing || position == null || !Double.isFinite(distanceToTarget) || distanceToTarget <= farDistance) {
            reset(currentTick, position, distanceToTarget);
            return false;
        }
        if (anchorPosition == null) {
            reset(currentTick, position, distanceToTarget);
            return false;
        }
        boolean moved = anchorPosition.distance(position) >= minimumProgress;
        boolean closedDistance = anchorDistance - distanceToTarget >= minimumProgress;
        if (moved || closedDistance) {
            reset(currentTick, position, distanceToTarget);
            return false;
        }
        stuck = currentTick - lastProgressTick >= stuckWindowTicks;
        return stuck;
    }

    boolean stuck() {
        return stuck;
    }

    private void reset(long tick, JeremyPosition position, double distance) {
        anchorPosition = position;
        anchorDistance = Double.isFinite(distance) ? distance : 0.0;
        lastProgressTick = tick;
        stuck = false;
    }
}

record JeremyPosition(double x, double y, double z) {
    double distance(JeremyPosition other) {
        double dx = other.x - x;
        double dy = other.y - y;
        double dz = other.z - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
