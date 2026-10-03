package dev.linqfy.bigCasares.modules.grapplinghook;

import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrappleShotMathTest {

    private static final double MAX_SPEED = GrapplingHookSettings.MAX_IMPULSE_SPEED;

    @Test
    void travelTicksCeilDistanceOverSpeed() {
        assertEquals(10, GrappleShotMath.travelTicks(16.0, 1.6));
        assertEquals(8, GrappleShotMath.travelTicks(12.0, 1.6));
        assertEquals(23, GrappleShotMath.travelTicks(36.0, 1.6));
        assertEquals(1, GrappleShotMath.travelTicks(1.0, 1.6));
    }

    @Test
    void travelTicksIsBoundedForDegenerateInputs() {
        assertEquals(1, GrappleShotMath.travelTicks(0.0, 1.6));
        assertEquals(1, GrappleShotMath.travelTicks(10.0, 0.0));
    }

    @Test
    void lerpSpansEndpoints() {
        Vector from = new Vector(0, 0, 0);
        Vector to = new Vector(10, 0, 0);
        assertEquals(0.0, GrappleShotMath.lerp(from, to, 0.0).getX(), 1.0e-9);
        assertEquals(10.0, GrappleShotMath.lerp(from, to, 1.0).getX(), 1.0e-9);
        assertEquals(5.0, GrappleShotMath.lerp(from, to, 0.5).getX(), 1.0e-9);
        // Clamped outside [0, 1].
        assertEquals(10.0, GrappleShotMath.lerp(from, to, 3.0).getX(), 1.0e-9);
    }

    @Test
    void chainPositionsAreSpacedAndEndOnTarget() {
        List<Vector> positions = GrappleShotMath.chainPositions(
            new Vector(0, 0, 0), new Vector(10, 0, 0), 1.0);

        assertEquals(10, positions.size());
        Vector last = positions.get(positions.size() - 1);
        assertEquals(10.0, last.getX(), 1.0e-9);
        for (int index = 0; index < positions.size(); index++) {
            assertEquals(index + 1.0, positions.get(index).getX(), 1.0e-9);
        }
    }

    @Test
    void chainPositionsFollowPartialDistances() {
        // A hook halfway out yields half the segments; the last one is the hook.
        List<Vector> partial = GrappleShotMath.chainPositions(
            new Vector(0, 0, 0), new Vector(5, 0, 0), 1.0);
        assertEquals(5, partial.size());
        assertEquals(5.0, partial.get(4).getX(), 1.0e-9);

        // Shorter than one spacing: a single segment on the target.
        List<Vector> tiny = GrappleShotMath.chainPositions(
            new Vector(0, 0, 0), new Vector(0.4, 0, 0), 1.0);
        assertEquals(1, tiny.size());
        assertEquals(0.4, tiny.get(0).getX(), 1.0e-9);
    }

    @Test
    void chainPositionsRejectNonPositiveSpacing() {
        assertThrows(IllegalArgumentException.class,
            () -> GrappleShotMath.chainPositions(new Vector(), new Vector(1, 0, 0), 0.0));
    }

    @Test
    void longDenseChainHasBoundedDisplaysWithoutLosingTarget() {
        Vector target = new Vector(256, 30, -10);
        List<Vector> positions = GrappleShotMath.chainPositions(new Vector(), target, 0.25);
        assertTrue(positions.size() <= 96, "visual chain must not allocate thousands of displays");
        assertEquals(target, positions.getLast());
    }

    @Test
    void chainRejectsNonFiniteSpacing() {
        assertThrows(IllegalArgumentException.class,
            () -> GrappleShotMath.chainPositions(new Vector(), new Vector(1, 0, 0), Double.NaN));
    }

    @Test
    void chainModelConnectsItsExactEndpointsInEveryDirection() {
        Vector from = new Vector(10000, 70, -20000);
        for (Vector offset : List.of(new Vector(8, 0, 0), new Vector(0, 8, 0),
            new Vector(0, -8, 0), new Vector(0, 0, -8), new Vector(4, -5, 7))) {
            Vector to = from.clone().add(offset);
            Vector midpoint = GrappleShotMath.lerp(from, to, 0.5);
            Matrix4f transform = GrappleShotMath.chainTransform(from, to);
            Vector3f bottom = transform.transformPosition(new Vector3f(0.5f, 0, 0.5f));
            Vector3f top = transform.transformPosition(new Vector3f(0.5f, 1, 0.5f));
            assertEquals(0.0, from.distance(midpoint.clone().add(new Vector(bottom.x, bottom.y, bottom.z))), 1e-5);
            assertEquals(0.0, to.distance(midpoint.clone().add(new Vector(top.x, top.y, top.z))), 1e-5);
            assertEquals(new Vector(10000, 70, -20000), from, "geometry must not modify the player's anchor");
        }
    }

    @Test
    void chainTransformRejectsDegenerateSegments() {
        assertThrows(IllegalArgumentException.class,
            () -> GrappleShotMath.chainTransform(new Vector(), new Vector()));
    }

    // ------------------------------------------------------------------
    // Pull impulse: direction = target - player, normalized, plus bias and
    // the safety clamp.
    // ------------------------------------------------------------------

    @Test
    void impulsePointsTowardTargetWithUpwardBias() {
        Vector impulse = GrappleShotMath.impulseVelocity(
            new Vector(0, 0, 0), new Vector(10, 0, 0), 1.1, 0.25, MAX_SPEED);

        assertEquals(1.1, impulse.getX(), 1.0e-9);
        assertEquals(0.25, impulse.getY(), 1.0e-9);
        assertEquals(0.0, impulse.getZ(), 1.0e-9);
    }

    @Test
    void impulseUsesNormalizedDirection() {
        Vector impulse = GrappleShotMath.impulseVelocity(
            new Vector(0, 0, 0), new Vector(30, 0, 0), 1.1, 0.25, MAX_SPEED);

        assertEquals(1.1, impulse.getX(), 1.0e-9);
        assertEquals(0.25, impulse.getY(), 1.0e-9);
    }

    @Test
    void zeroLengthImpulseFallsBackToUpward() {
        Vector impulse = GrappleShotMath.impulseVelocity(
            new Vector(1, 2, 3), new Vector(1, 2, 3), 1.0, 0.25, MAX_SPEED);

        assertTrue(Math.abs(impulse.getX()) < 1.0e-9);
        assertEquals(1.25, impulse.getY(), 1.0e-9);
        assertTrue(Math.abs(impulse.getZ()) < 1.0e-9);
    }

    @Test
    void horizontalTargetPushesClearlyTowardTheWall() {
        Vector impulse = GrappleShotMath.impulseVelocity(
            new Vector(0, 0, 0), new Vector(10, 0, 0), 2.4, 0.31, MAX_SPEED);

        assertEquals(2.4, impulse.getX(), 1.0e-9);
        // The flat shot must go mostly sideways: the vertical bias is a small
        // arc assist, never the dominant component.
        assertEquals(0.31, impulse.getY(), 1.0e-9);
        assertTrue(impulse.getX() > 3.0 * impulse.getY());
        // And the pull is still aimed at the target (no exaggerated bias).
        double toward = impulse.clone().normalize().dot(new Vector(1, 0, 0));
        assertTrue(toward > 0.97, "impulse must point mostly at the target, got dot=" + toward);
    }

    @Test
    void elevatedTargetClimbsClearly() {
        Vector impulse = GrappleShotMath.impulseVelocity(
            new Vector(0, 0, 0), new Vector(0, 10, 0), 2.8, 0.35, MAX_SPEED);

        // Full upward direction scaled by power, plus the bias.
        assertEquals(0.0, impulse.getX(), 1.0e-9);
        assertEquals(3.15, impulse.getY(), 1.0e-9);
        assertEquals(0.0, impulse.getZ(), 1.0e-9);
    }

    @Test
    void lowTargetDivesTowardIt() {
        Vector impulse = GrappleShotMath.impulseVelocity(
            new Vector(0, 0, 0), new Vector(0, -10, 0), 2.8, 0.35, MAX_SPEED);

        // Downward direction minus the small bias still dives clearly.
        assertTrue(impulse.getY() < 0.0, "downward shot must descend, got y=" + impulse.getY());
        assertEquals(-2.45, impulse.getY(), 1.0e-9);
        assertTrue(Math.abs(impulse.getY()) > 2.0);
    }

    @Test
    void diagonalTargetIsPulledAlongTheDiagonal() {
        Vector from = new Vector(0, 0, 0);
        Vector to = new Vector(10, 5, 3);
        Vector impulse = GrappleShotMath.impulseVelocity(from, to, 2.4, 0.31, MAX_SPEED);

        Vector expectedDirection = to.clone().normalize();
        Vector actualDirection = impulse.clone().normalize();
        double dot = actualDirection.dot(expectedDirection);
        assertTrue(dot > 0.99, "diagonal impulse must follow the target direction, dot=" + dot);
        // Every component keeps its sign: forward, up and sideways.
        assertTrue(impulse.getX() > 0.0 && impulse.getY() > 0.0 && impulse.getZ() > 0.0);
    }

    @Test
    void impulseIsClampedToTheSafetyCeiling() {
        // Absurd configuration: a 100-blocks-per-tick power and a huge bias
        // must never escape the hard clamp.
        Vector impulse = GrappleShotMath.impulseVelocity(
            new Vector(0, 0, 0), new Vector(100, 0, 0), 100.0, 5.0, MAX_SPEED);

        assertEquals(MAX_SPEED, impulse.length(), 1.0e-9);
    }

    @Test
    void impulseBelowCeilingIsNotClamped() {
        Vector impulse = GrappleShotMath.impulseVelocity(
            new Vector(0, 0, 0), new Vector(10, 0, 0), 1.8, 0.25, MAX_SPEED);

        assertEquals(Math.sqrt(1.8 * 1.8 + 0.25 * 0.25), impulse.length(), 1.0e-9);
        assertTrue(impulse.length() <= MAX_SPEED);
    }

    // ------------------------------------------------------------------
    // End of range: where a miss flies to.
    // ------------------------------------------------------------------

    @Test
    void endOfRangeStopsExactlyAtTheTierRange() {
        Vector start = new Vector(10, 20, 30);
        Vector direction = new Vector(1, 0, 0);
        Vector end = GrappleShotMath.endOfRange(start, direction, 12.0);

        assertEquals(22.0, end.getX(), 1.0e-9);
        assertEquals(20.0, end.getY(), 1.0e-9);
        assertEquals(30.0, end.getZ(), 1.0e-9);
        assertEquals(12.0, start.distance(end), 1.0e-9);
    }

    @Test
    void endOfRangeUsesTheFullLookDirectionIncludingVertical() {
        Vector start = new Vector(0, 0, 0);
        Vector end = GrappleShotMath.endOfRange(start, new Vector(0, 1, 0), 36.0);

        assertEquals(0.0, end.getX(), 1.0e-9);
        assertEquals(36.0, end.getY(), 1.0e-9);
        assertEquals(0.0, end.getZ(), 1.0e-9);
    }

    @Test
    void endOfRangeNormalizesNonUnitDirections() {
        Vector start = new Vector(0, 0, 0);
        Vector end = GrappleShotMath.endOfRange(start, new Vector(3, 4, 0), 10.0);

        assertEquals(6.0, end.getX(), 1.0e-9);
        assertEquals(8.0, end.getY(), 1.0e-9);
        assertEquals(10.0, start.distance(end), 1.0e-9);
    }

    @Test
    void endOfRangeHandlesZeroDirection() {
        Vector start = new Vector(1, 2, 3);
        Vector end = GrappleShotMath.endOfRange(start, new Vector(0, 0, 0), 10.0);

        assertEquals(1.0, end.getX(), 1.0e-9);
        assertEquals(2.0, end.getY(), 1.0e-9);
        assertEquals(3.0, end.getZ(), 1.0e-9);
    }

    // ------------------------------------------------------------------
    // Nearest target: first valid target along the ray wins.
    // ------------------------------------------------------------------

    @Test
    void entityInFrontOfWallWins() {
        // jugador -> entidad(3) -> pared(8): debe enganchar la entidad.
        assertEquals(GrappleShotMath.HitWinner.ENTITY,
            GrappleShotMath.nearestTarget(true, 8.0, true, 3.0));
    }

    @Test
    void wallInFrontOfEntityWins() {
        // jugador -> pared(3) -> entidad(8): debe enganchar la pared.
        assertEquals(GrappleShotMath.HitWinner.BLOCK,
            GrappleShotMath.nearestTarget(true, 3.0, true, 8.0));
    }

    @Test
    void entityBehindWallIsNeverGrabbed() {
        // jugador -> pared(5) -> entidad(8): la entidad detrás de la pared no
        // puede atravesar el bloque.
        assertEquals(GrappleShotMath.HitWinner.BLOCK,
            GrappleShotMath.nearestTarget(true, 5.0, true, 8.0));
    }

    @Test
    void blockOnlyAndEntityOnlyShots() {
        assertEquals(GrappleShotMath.HitWinner.BLOCK,
            GrappleShotMath.nearestTarget(true, 5.0, false, Double.MAX_VALUE));
        assertEquals(GrappleShotMath.HitWinner.ENTITY,
            GrappleShotMath.nearestTarget(false, Double.MAX_VALUE, true, 4.0));
    }

    @Test
    void noTargetWithinRangeIsAMiss() {
        assertEquals(GrappleShotMath.HitWinner.NONE,
            GrappleShotMath.nearestTarget(false, Double.MAX_VALUE, false, Double.MAX_VALUE));
        // Out-of-range results are reported as misses even if one side has a hit.
        assertEquals(GrappleShotMath.HitWinner.NONE,
            GrappleShotMath.nearestTarget(true, Double.MAX_VALUE, true, Double.MAX_VALUE));
    }

    // ------------------------------------------------------------------
    // Sustained winch pull (player to block anchor, entity to player).
    // ------------------------------------------------------------------

    @Test
    void sustainPullsStraightTowardTheTargetAtTheGivenPower() {
        Vector pull = GrappleShotMath.sustainVelocity(new Vector(0, 0, 0), new Vector(10, 0, 0), 1.5);

        assertEquals(1.5, pull.getX(), 1.0e-9);
        assertEquals(0.0, pull.getY(), 1.0e-9);
        assertEquals(1.5, pull.length(), 1.0e-9);
    }

    @Test
    void sustainUsesTheFullDirectionIncludingVertical() {
        // Pulling an entity up from below must have a strong vertical part.
        Vector pull = GrappleShotMath.sustainVelocity(new Vector(0, 0, 0), new Vector(0, 10, 5), 1.2);

        Vector expected = new Vector(0, 10, 5).normalize().multiply(1.2);
        assertEquals(expected.getX(), pull.getX(), 1.0e-9);
        assertEquals(expected.getY(), pull.getY(), 1.0e-9);
        assertEquals(expected.getZ(), pull.getZ(), 1.0e-9);
        assertTrue(pull.getY() > pull.getZ() && pull.getY() > 0.0, "vertical pull must dominate");
    }

    @Test
    void sustainIsZeroWhenAlreadyOnTheTarget() {
        Vector pull = GrappleShotMath.sustainVelocity(new Vector(1, 2, 3), new Vector(1, 2, 3), 5.0);

        assertEquals(0.0, pull.length(), 1.0e-9);
    }

    @Test
    void sustainNeverGoesBackwardsWithNegativePower() {
        Vector pull = GrappleShotMath.sustainVelocity(new Vector(0, 0, 0), new Vector(10, 0, 0), -3.0);

        assertEquals(0.0, pull.length(), 1.0e-9);
    }

    @Test
    void invalidDistancesAreIgnored() {
        // NaN/negative distances must be treated as "no hit", not crash.
        assertEquals(GrappleShotMath.HitWinner.NONE,
            GrappleShotMath.nearestTarget(true, Double.NaN, false, Double.MAX_VALUE));
        assertEquals(GrappleShotMath.HitWinner.NONE,
            GrappleShotMath.nearestTarget(true, -1.0, true, -2.0));
        // A valid target still wins when the other side is invalid.
        assertEquals(GrappleShotMath.HitWinner.ENTITY,
            GrappleShotMath.nearestTarget(true, Double.NaN, true, 3.0));
        assertEquals(GrappleShotMath.HitWinner.BLOCK,
            GrappleShotMath.nearestTarget(true, 5.0, true, Double.NaN));
    }
}
