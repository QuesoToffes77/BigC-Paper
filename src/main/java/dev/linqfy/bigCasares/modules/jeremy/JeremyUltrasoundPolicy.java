package dev.linqfy.bigCasares.modules.jeremy;

final class JeremyUltrasoundPolicy {
    private JeremyUltrasoundPolicy() {
    }

    static boolean canCharge(
        boolean hunting,
        boolean targetAlive,
        boolean sameWorld,
        boolean stuck,
        double distance,
        double range,
        boolean cooldownReady
    ) {
        return hunting && targetAlive && sameWorld && stuck && cooldownReady
            && Double.isFinite(distance) && distance >= 0.0 && distance <= range;
    }

    static JeremyVector direction(JeremyVector origin, JeremyVector target) {
        return target.subtract(origin).normalize();
    }

    static boolean canReach(boolean directLine, int solidBlocks, JeremyWallPenetrationSettings settings) {
        if (directLine) {
            return true;
        }
        return settings != null && settings.enabled() && solidBlocks >= 0 && solidBlocks <= settings.maxBlocks();
    }
}

record JeremyVector(double x, double y, double z) {
    JeremyVector subtract(JeremyVector other) {
        return new JeremyVector(x - other.x, y - other.y, z - other.z);
    }

    JeremyVector normalize() {
        double length = Math.sqrt(x * x + y * y + z * z);
        return length <= 1.0E-9 ? new JeremyVector(0, 0, 0) : new JeremyVector(x / length, y / length, z / length);
    }
}
