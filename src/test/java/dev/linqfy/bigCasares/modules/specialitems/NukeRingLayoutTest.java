package dev.linqfy.bigCasares.modules.specialitems;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NukeRingLayoutTest {

    @Test
    void buildsFiveExponentiallyGrowingRingsAroundOneCenterForTwoHundredTotalTnt() {
        List<NukeRing> rings = NukeRingLayout.create(5, 199, 2.0);

        assertEquals(5, rings.size());
        assertEquals(List.of(6, 13, 26, 51, 103),
            rings.stream().map(ring -> ring.points().size()).toList());
        assertEquals(199, rings.stream().mapToInt(ring -> ring.points().size()).sum());

        Set<String> coordinates = new HashSet<>();
        rings.stream().flatMap(ring -> ring.points().stream()).forEach(point ->
            coordinates.add("%.6f:%.6f".formatted(point.x(), point.z())));
        assertEquals(199, coordinates.size());
    }

    @Test
    void increasesRadiusAndSpacesEveryRingEvenlyByAngle() {
        List<NukeRing> rings = NukeRingLayout.create(5, 199, 2.0);

        for (int index = 0; index < rings.size(); index++) {
            NukeRing ring = rings.get(index);
            assertEquals((index + 1) * 2.0, ring.radius(), 0.000001);
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
