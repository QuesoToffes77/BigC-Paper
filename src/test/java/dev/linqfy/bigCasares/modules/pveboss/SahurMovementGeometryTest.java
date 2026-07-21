package dev.linqfy.bigCasares.modules.pveboss;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SahurMovementGeometryTest {

    @Test
    void betterModelAnchorFacesOppositeItsVisualTravelDirection() {
        assertEquals(-0.6, SahurMovementGeometry.anchorDirectionX(0.6));
        assertEquals(0.8, SahurMovementGeometry.anchorDirectionZ(-0.8));
    }

    @Test
    void choosesHighestNearbyFloorWithTwoBlocksOfHeadroom() {
        Set<Integer> solid = Set.of(8, 10);
        Set<Integer> passable = Set.of(9, 11, 12);

        assertEquals(OptionalInt.of(11), SahurMovementGeometry.findFeetY(
            10,
            solid::contains,
            passable::contains
        ));
    }

    @Test
    void skipsFloorBlockedByLowCeilingAndUsesLowerSafeFloor() {
        Set<Integer> solid = Set.of(8, 10, 12);
        Set<Integer> passable = Set.of(9, 10, 11);

        assertEquals(OptionalInt.of(9), SahurMovementGeometry.findFeetY(
            11,
            solid::contains,
            passable::contains
        ));
    }

    @Test
    void leavesEntityToGravityWhenThereIsNoFloorNearby() {
        assertEquals(OptionalInt.empty(), SahurMovementGeometry.findFeetY(
            10,
            ignored -> false,
            ignored -> true
        ));
    }
}
