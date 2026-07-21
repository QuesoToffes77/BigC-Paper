package dev.linqfy.bigCasares.modules.pveboss;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SahurChargePlannerTest {

    @Test
    void choosesSolidWallCorridorContainingMostPlayers() {
        var planner = new SahurChargePlanner(8.0, 40.0, 2.25);
        List<SahurChargePlanner.Corridor> corridors = List.of(
            corridor(1, 0, 20, true),
            corridor(0, 1, 16, true),
            corridor(-1, 0, 30, false)
        );
        List<BossPosition> players = List.of(pos(5, 1), pos(9, -1), pos(0, 7));

        assertEquals(corridors.getFirst(), planner.choose(pos(0, 0), corridors, players).orElseThrow());
    }

    @Test
    void rejectsOpenAirAndWallsOutsideDistanceBounds() {
        var planner = new SahurChargePlanner(8.0, 40.0, 2.25);

        assertTrue(planner.choose(pos(0, 0), List.of(
            corridor(1, 0, 7, true),
            corridor(0, 1, 41, true),
            corridor(-1, 0, 20, false)
        ), List.of()).isEmpty());
    }

    @Test
    void countsHorizontalPointsOnlyWithinTheOriginToWallSegment() {
        var planner = new SahurChargePlanner(8.0, 40.0, 2.25);
        SahurChargePlanner.Corridor east = corridor(1, 0, 20, true);
        SahurChargePlanner.Corridor north = corridor(0, 1, 20, true);
        List<BossPosition> players = List.of(
            pos(10, 2),
            pos(-1, 0),
            pos(21, 0),
            pos(0, 10)
        );

        assertEquals(north, planner.choose(pos(0, 0), List.of(east, north), players).orElseThrow());
    }

    @Test
    void prefersShorterCorridorWhenPlayerCountsTie() {
        var planner = new SahurChargePlanner(8.0, 40.0, 2.25);
        SahurChargePlanner.Corridor longCorridor = corridor(1, 0, 30, true);
        SahurChargePlanner.Corridor shortCorridor = corridor(0, 1, 12, true);

        assertEquals(shortCorridor, planner.choose(
            pos(0, 0),
            List.of(longCorridor, shortCorridor),
            List.of(pos(5, 1), pos(1, 5))
        ).orElseThrow());
    }

    private static SahurChargePlanner.Corridor corridor(
        double directionX,
        double directionZ,
        double length,
        boolean solidImpact
    ) {
        return new SahurChargePlanner.Corridor(directionX, directionZ, length, solidImpact);
    }

    private static BossPosition pos(double x, double z) {
        return new BossPosition(x, 0.0, z);
    }
}
