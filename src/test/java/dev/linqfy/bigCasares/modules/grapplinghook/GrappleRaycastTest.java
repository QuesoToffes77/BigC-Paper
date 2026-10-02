package dev.linqfy.bigCasares.modules.grapplinghook;

import dev.linqfy.bigCasares.modules.grapplinghook.GrappleRaycast.VoxelHit;
import dev.linqfy.bigCasares.modules.grapplinghook.GrappleShotMath.HitWinner;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The block ray is a deterministic voxel traversal from the eye along the
 * full camera direction, so every orientation behaves the same: wall, floor,
 * ceiling, diagonal, up, down, within/out of range, and entity-vs-block
 * ordering.
 */
class GrappleRaycastTest {

    private static final Vector ORIGIN = new Vector(0, 0, 0);

    @Test
    void horizontalWallIsHit() {
        Optional<VoxelHit> hit = GrappleRaycast.firstSolid(
            ORIGIN, new Vector(1, 0, 0), 12.0, (x, y, z) -> x == 10);

        assertTrue(hit.isPresent());
        assertEquals(10, hit.get().x());
        assertEquals(10.0, hit.get().distance(), 1.0e-9);
        assertEquals(10.0, hit.get().entryPoint().getX(), 1.0e-9);
        assertEquals(0.0, hit.get().entryPoint().getY(), 1.0e-9);
    }

    @Test
    void diagonalWallIsHit() {
        Optional<VoxelHit> hit = GrappleRaycast.firstSolid(
            ORIGIN, new Vector(1, 0, 1), 12.0, (x, y, z) -> x == 5 && z == 5);

        assertTrue(hit.isPresent());
        assertEquals(5, hit.get().x());
        assertEquals(5, hit.get().z());
        // The voxel is 5 blocks away along the diagonal, so the ray distance
        // is 5 * sqrt(2) and the entry point is exactly the voxel corner.
        assertEquals(5.0 * Math.sqrt(2.0), hit.get().distance(), 1.0e-9);
        assertEquals(5.0 * Math.sqrt(2.0), ORIGIN.distance(hit.get().entryPoint()), 1.0e-9);
    }

    @Test
    void ceilingIsHit() {
        Optional<VoxelHit> hit = GrappleRaycast.firstSolid(
            ORIGIN, new Vector(0, 1, 0), 12.0, (x, y, z) -> y == 8);

        assertTrue(hit.isPresent());
        assertEquals(8, hit.get().y());
        assertEquals(8.0, hit.get().distance(), 1.0e-9);
    }

    @Test
    void floorIsHitFromEyeHeight() {
        // Player eye at y=1.62 looking straight down; the floor block below
        // spans y in [0, 1]. The hook must enter its top face.
        Optional<VoxelHit> hit = GrappleRaycast.firstSolid(
            new Vector(0, 1.62, 0), new Vector(0, -1, 0), 12.0, (x, y, z) -> y == 0);

        assertTrue(hit.isPresent());
        assertEquals(0, hit.get().y());
        assertEquals(0.62, hit.get().distance(), 1.0e-9);
        assertEquals(1.0, hit.get().entryPoint().getY(), 1.0e-9);
    }

    @Test
    void targetBeyondRangeIsNeverHit() {
        Optional<VoxelHit> hit = GrappleRaycast.firstSolid(
            ORIGIN, new Vector(1, 0, 0), 12.0, (x, y, z) -> x == 20);

        assertFalse(hit.isPresent());
    }

    @Test
    void targetWithinRangeIsHit() {
        Optional<VoxelHit> hit = GrappleRaycast.firstSolid(
            ORIGIN, new Vector(1, 0, 0), 12.0, (x, y, z) -> x == 5);

        assertTrue(hit.isPresent());
        assertEquals(5.0, hit.get().distance(), 1.0e-9);
    }

    @Test
    void passableBlocksAreSkippedUntilTheFirstSolidOne() {
        // The ray crosses voxels 1..9 as air, then stops at x=10.
        Optional<VoxelHit> hit = GrappleRaycast.firstSolid(
            ORIGIN, new Vector(1, 0, 0), 12.0, (x, y, z) -> x >= 10);

        assertTrue(hit.isPresent());
        assertEquals(10.0, hit.get().distance(), 1.0e-9);
    }

    @Test
    void aabbEntityInFrontOfTheWallWins() {
        Optional<VoxelHit> block = GrappleRaycast.firstSolid(
            ORIGIN, new Vector(1, 0, 0), 12.0, (x, y, z) -> x == 8);
        Optional<Double> entity = GrappleRaycast.intersectAabb(
            ORIGIN, new Vector(1, 0, 0), new Vector(3, 0, 0), new Vector(4, 2, 2));

        assertTrue(entity.isPresent());
        assertEquals(3.0, entity.get(), 1.0e-9);
        assertEquals(HitWinner.ENTITY,
            GrappleShotMath.nearestTarget(block.isPresent(), block.get().distance(),
                entity.isPresent(), entity.get()));
    }

    @Test
    void wallInFrontOfTheEntityWins() {
        Optional<VoxelHit> block = GrappleRaycast.firstSolid(
            ORIGIN, new Vector(1, 0, 0), 12.0, (x, y, z) -> x == 3);
        Optional<Double> entity = GrappleRaycast.intersectAabb(
            ORIGIN, new Vector(1, 0, 0), new Vector(8, 0, 0), new Vector(9, 2, 2));

        assertTrue(entity.isPresent());
        assertEquals(8.0, entity.get(), 1.0e-9);
        assertEquals(HitWinner.BLOCK,
            GrappleShotMath.nearestTarget(block.isPresent(), block.get().distance(),
                entity.isPresent(), entity.get()));
    }

    @Test
    void entityBehindAWallIsNeverGrabbed() {
        Optional<VoxelHit> block = GrappleRaycast.firstSolid(
            ORIGIN, new Vector(1, 0, 0), 12.0, (x, y, z) -> x == 5);
        Optional<Double> entity = GrappleRaycast.intersectAabb(
            ORIGIN, new Vector(1, 0, 0), new Vector(8, 0, 0), new Vector(9, 2, 2));

        assertEquals(HitWinner.BLOCK,
            GrappleShotMath.nearestTarget(block.isPresent(), block.get().distance(),
                entity.isPresent(), entity.get()));
    }

    @Test
    void aabbIntersectionDistances() {
        // Origin already inside the box: hit at distance 0.
        assertEquals(0.0, GrappleRaycast.intersectAabb(
            ORIGIN, new Vector(1, 0, 0), new Vector(-1, -1, -1), new Vector(1, 1, 1)).get(), 1.0e-9);
        // Box off the ray path: miss.
        assertTrue(GrappleRaycast.intersectAabb(
            ORIGIN, new Vector(1, 0, 0), new Vector(0, 5, 0), new Vector(2, 7, 2)).isEmpty());
        // Box far beyond any range: still measurable; the caller filters it.
        Optional<Double> far = GrappleRaycast.intersectAabb(
            ORIGIN, new Vector(1, 0, 0), new Vector(50, 0, 0), new Vector(51, 2, 2));
        assertTrue(far.isPresent());
        assertTrue(far.get() > 49.0);
        // Diagonal ray into a box: entry distance = 3 * sqrt(2).
        Optional<Double> diagonal = GrappleRaycast.intersectAabb(
            ORIGIN, new Vector(1, 1, 0), new Vector(3, 3, 0), new Vector(4, 4, 2));
        assertTrue(diagonal.isPresent());
        assertEquals(3.0 * Math.sqrt(2.0), diagonal.get(), 1.0e-9);
    }
}
