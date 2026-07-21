package dev.linqfy.bigCasares.modules.specialitems;

import java.util.ArrayList;
import java.util.List;

public final class NukeRingLayout {

    private NukeRingLayout() {
    }

    public static List<NukeRing> create(int ringCount, int totalPoints, double radiusStep) {
        if (ringCount <= 0 || ringCount > 30 || radiusStep <= 0.0) {
            throw new IllegalArgumentException("ring count and radius step must be positive");
        }

        int[] populations = populations(ringCount, totalPoints);
        List<NukeRing> rings = new ArrayList<>(ringCount);
        for (int ringIndex = 0; ringIndex < ringCount; ringIndex++) {
            double radius = radiusStep * (ringIndex + 1);
            int population = populations[ringIndex];
            List<NukePoint> points = new ArrayList<>(population);
            for (int pointIndex = 0; pointIndex < population; pointIndex++) {
                double angle = Math.PI * 2.0 * pointIndex / population;
                points.add(new NukePoint(Math.cos(angle) * radius, Math.sin(angle) * radius));
            }
            rings.add(new NukeRing(radius, points));
        }
        return List.copyOf(rings);
    }

    private static int[] populations(int ringCount, int totalPoints) {
        long weightTotal = (1L << ringCount) - 1L;
        if (totalPoints < weightTotal) {
            throw new IllegalArgumentException("total points cannot form exponential rings");
        }

        int[] values = new int[ringCount];
        long[] remainders = new long[ringCount];
        int assigned = 0;
        for (int index = 0; index < ringCount; index++) {
            long weighted = (long) totalPoints * (1L << index);
            values[index] = (int) (weighted / weightTotal);
            remainders[index] = weighted % weightTotal;
            assigned += values[index];
        }
        while (assigned < totalPoints) {
            int winner = 0;
            for (int index = 1; index < ringCount; index++) {
                if (remainders[index] >= remainders[winner]) {
                    winner = index;
                }
            }
            values[winner]++;
            remainders[winner] = -1L;
            assigned++;
        }
        return values;
    }
}
