package dev.linqfy.bigCasares.modules.tombstone;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

final class TombstoneProximity {
    private TombstoneProximity() {
    }

    static Optional<TombstoneRecord> nearest(
        Collection<TombstoneRecord> records,
        UUID worldId,
        double x,
        double y,
        double z,
        double radius
    ) {
        return nearby(records, worldId, x, y, z, radius).stream().findFirst();
    }

    static List<TombstoneRecord> nearby(
        Collection<TombstoneRecord> records,
        UUID worldId,
        double x,
        double y,
        double z,
        double radius
    ) {
        if (radius < 0.0) throw new IllegalArgumentException("radius cannot be negative");
        double radiusSquared = radius * radius;
        return records.stream()
            .filter(record -> record.worldId().equals(worldId))
            .filter(record -> distanceSquared(record, x, y, z) <= radiusSquared)
            .sorted(Comparator.comparingDouble(record -> distanceSquared(record, x, y, z)))
            .toList();
    }

    private static double distanceSquared(TombstoneRecord record, double x, double y, double z) {
        double dx = record.x() - x;
        double dy = record.y() - y;
        double dz = record.z() - z;
        return dx * dx + dy * dy + dz * dz;
    }
}
