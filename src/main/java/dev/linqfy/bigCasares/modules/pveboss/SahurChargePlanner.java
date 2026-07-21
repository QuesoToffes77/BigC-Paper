package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SahurChargePlanner {

    private final double minimumLength;
    private final double maximumLength;
    private final double halfWidth;

    public SahurChargePlanner(double minimumLength, double maximumLength, double halfWidth) {
        if (!Double.isFinite(minimumLength)
            || !Double.isFinite(maximumLength)
            || !Double.isFinite(halfWidth)
            || minimumLength < 0.0
            || maximumLength < minimumLength
            || halfWidth < 0.0) {
            throw new IllegalArgumentException("invalid charge corridor bounds");
        }
        this.minimumLength = minimumLength;
        this.maximumLength = maximumLength;
        this.halfWidth = halfWidth;
    }

    public Optional<Corridor> choose(
        BossPosition origin,
        List<Corridor> candidates,
        List<BossPosition> players
    ) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(players, "players");

        return candidates.stream()
            .filter(Corridor::solidImpact)
            .filter(corridor -> corridor.length() >= minimumLength && corridor.length() <= maximumLength)
            .max(Comparator.comparingInt((Corridor corridor) -> playersInCorridor(origin, corridor, players))
                .thenComparing(Comparator.comparingDouble(Corridor::length).reversed()));
    }

    private int playersInCorridor(BossPosition origin, Corridor corridor, List<BossPosition> players) {
        double directionMagnitude = Math.hypot(corridor.directionX(), corridor.directionZ());
        double unitX = corridor.directionX() / directionMagnitude;
        double unitZ = corridor.directionZ() / directionMagnitude;
        double maximumDistanceSquared = halfWidth * halfWidth;
        int count = 0;

        for (BossPosition player : players) {
            double offsetX = player.x() - origin.x();
            double offsetZ = player.z() - origin.z();
            double projection = offsetX * unitX + offsetZ * unitZ;
            if (projection < 0.0 || projection > corridor.length()) {
                continue;
            }

            double perpendicularX = offsetX - projection * unitX;
            double perpendicularZ = offsetZ - projection * unitZ;
            if (perpendicularX * perpendicularX + perpendicularZ * perpendicularZ <= maximumDistanceSquared) {
                count++;
            }
        }
        return count;
    }

    public record Corridor(double directionX, double directionZ, double length, boolean solidImpact) {

        public Corridor {
            if (!Double.isFinite(directionX)
                || !Double.isFinite(directionZ)
                || !Double.isFinite(length)
                || (directionX == 0.0 && directionZ == 0.0)
                || length < 0.0) {
                throw new IllegalArgumentException("invalid charge corridor");
            }
        }
    }
}
