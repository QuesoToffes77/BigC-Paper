package dev.linqfy.bigCasares.modules.specialitems;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NukeRingLayoutTest {

    @Test
    void buildsSixExponentiallyGrowingRingsAroundOneCenterForThreeHundredFiftyTotalTnt() {
        List<NukeRing> rings = NukeRingLayout.create(6, 349, 3.0);

        assertEquals(6, rings.size());
        assertEquals(List.of(6, 11, 22, 44, 89, 177),
            rings.stream().map(ring -> ring.points().size()).toList());
        assertEquals(349, rings.stream().mapToInt(ring -> ring.points().size()).sum());

        Set<String> coordinates = new HashSet<>();
        rings.stream().flatMap(ring -> ring.points().stream()).forEach(point ->
            coordinates.add("%.6f:%.6f".formatted(point.x(), point.z())));
        assertEquals(349, coordinates.size());
    }

    @Test
    void increasesRadiusByThreeBlocksAndSpacesEveryRingEvenlyByAngle() {
        List<NukeRing> rings = NukeRingLayout.create(6, 349, 3.0);

        for (int index = 0; index < rings.size(); index++) {
            NukeRing ring = rings.get(index);
            assertEquals((index + 1) * 3.0, ring.radius(), 0.000001);
            double expectedChord = 2.0 * ring.radius() * Math.sin(Math.PI / ring.points().size());
            for (int pointIndex = 0; pointIndex < ring.points().size(); pointIndex++) {
                NukePoint first = ring.points().get(pointIndex);
                NukePoint second = ring.points().get((pointIndex + 1) % ring.points().size());
                assertEquals(expectedChord, Math.hypot(first.x() - second.x(), first.z() - second.z()), 0.000001);
            }
        }
    }

    @Test
    void rejectsTotalsThatCannotCreateExponentialRings() {
        boolean rejected = false;
        try {
            NukeRingLayout.create(5, 30, 2.0);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        assertTrue(rejected);
    }
}
