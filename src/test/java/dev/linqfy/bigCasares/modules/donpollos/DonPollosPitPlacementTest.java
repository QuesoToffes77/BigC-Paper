package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosPitPlacementTest {

    @Test
    void anAllowedChunkStaysWhereThePlayerIs() {
        assertArrayEquals(new int[]{5, -3}, DonPollosPitPlacement.nearestAllowed(5, -3, 4, (x, z) -> true).orElseThrow());
    }

    @Test
    void aProtectedChunkMovesToTheNearestAllowedOne() {
        // zona protegida: los chunks con x <= 6 (el jugador esta en x = 5)
        int[] spot = DonPollosPitPlacement.nearestAllowed(5, 0, 4, (x, z) -> x > 6).orElseThrow();
        assertArrayEquals(new int[]{7, 0}, spot, "dos chunks al este, en la misma fila");
    }

    @Test
    void closerByRealDistanceWinsAndTiesAlwaysPickTheSame() {
        Set<String> blocked = Set.of("0,0", "1,0", "-1,0", "0,1", "0,-1");
        int[] first = DonPollosPitPlacement.nearestAllowed(0, 0, 4, (x, z) -> !blocked.contains(x + "," + z)).orElseThrow();
        int[] again = DonPollosPitPlacement.nearestAllowed(0, 0, 4, (x, z) -> !blocked.contains(x + "," + z)).orElseThrow();
        assertArrayEquals(first, again);
        assertTrue(Math.abs(first[0]) == 1 && Math.abs(first[1]) == 1, "las diagonales estan mas cerca que a 2 chunks");
    }

    @Test
    void candidatesGoFromTheSameChunkOutwards() {
        java.util.List<int[]> chunks = DonPollosPitPlacement.byDistance(10, 10, 4);
        assertArrayEquals(new int[]{10, 10}, chunks.getFirst());
        org.junit.jupiter.api.Assertions.assertEquals(81, chunks.size());
        int last = 0;
        for (int[] chunk : chunks) {
            int distance = (chunk[0] - 10) * (chunk[0] - 10) + (chunk[1] - 10) * (chunk[1] - 10);
            assertTrue(distance >= last);
            last = distance;
        }
    }

    @Test
    void nothingAllowedNearbyMeansNoPit() {
        assertTrue(DonPollosPitPlacement.nearestAllowed(0, 0, 4, (x, z) -> false).isEmpty());
        assertTrue(DonPollosPitPlacement.nearestAllowed(0, 0, 4, (x, z) -> Math.abs(x) > 4).isEmpty(), "fuera del radio");
    }
}
