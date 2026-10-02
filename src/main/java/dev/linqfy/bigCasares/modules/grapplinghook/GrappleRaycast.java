package dev.linqfy.bigCasares.modules.grapplinghook;

import org.bukkit.util.Vector;

import java.util.Optional;

/**
 * Deterministic, orientation-independent ray casting. A pure voxel traversal
 * (Amanatides &amp; Woo DDA) walks the exact blocks the ray crosses from the
 * shooter's eye along the full camera direction (X, Y and Z kept), so a shot
 * hits the first solid block whether it is a floor, a wall, a ceiling or any
 * diagonal. This does not depend on Bukkit's {@code getTargetBlock} or on
 * {@code World#rayTrace*} internals; the only Bukkit input is the block
 * solidity tester supplied by the caller. Entity testing uses a pure
 * ray-vs-AABB slab intersection. Everything here is unit-testable without a
 * server.
 */
public final class GrappleRaycast {

    /** A block the ray crosses, with the exact entry point and distance. */
    public record VoxelHit(int x, int y, int z, double distance, Vector entryPoint) {
        public VoxelHit {
            entryPoint = entryPoint.clone();
        }
    }

    /** Decides whether the voxel at {@code (x, y, z)} stops the hook. */
    @FunctionalInterface
    public interface BlockTester {
        boolean isSolid(int x, int y, int z);
    }

    private GrappleRaycast() {
    }

    /**
     * First solid block along {@code direction} from {@code start}, up to
     * {@code maxDistance} blocks. Returns the voxel coordinates, the distance
     * along the ray and the precise point where the ray enters that voxel
     * (the face of the block the hook sticks to). Empty when the ray ends
     * without crossing a solid block or when the direction is degenerate.
     */
    public static Optional<VoxelHit> firstSolid(Vector start, Vector direction, double maxDistance,
                                                BlockTester tester) {
        Vector dir = direction.clone().normalize();
        if (dir.lengthSquared() <= 1.0e-9 || maxDistance <= 0.0) {
            return Optional.empty();
        }
        int x = floor(start.getX());
        int y = floor(start.getY());
        int z = floor(start.getZ());

        int stepX = step(dir.getX());
        int stepY = step(dir.getY());
        int stepZ = step(dir.getZ());

        double tDeltaX = dir.getX() != 0.0 ? Math.abs(1.0 / dir.getX()) : Double.POSITIVE_INFINITY;
        double tDeltaY = dir.getY() != 0.0 ? Math.abs(1.0 / dir.getY()) : Double.POSITIVE_INFINITY;
        double tDeltaZ = dir.getZ() != 0.0 ? Math.abs(1.0 / dir.getZ()) : Double.POSITIVE_INFINITY;

        double tMaxX = tMax(start.getX(), x, dir.getX());
        double tMaxY = tMax(start.getY(), y, dir.getY());
        double tMaxZ = tMax(start.getZ(), z, dir.getZ());

        double t = 0.0;
        while (t <= maxDistance) {
            if (tester.isSolid(x, y, z)) {
                return Optional.of(new VoxelHit(x, y, z, t, start.clone().add(dir.clone().multiply(t))));
            }
            if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
                x += stepX;
                t = tMaxX;
                tMaxX += tDeltaX;
            } else if (tMaxY <= tMaxZ) {
                y += stepY;
                t = tMaxY;
                tMaxY += tDeltaY;
            } else {
                z += stepZ;
                t = tMaxZ;
                tMaxZ += tDeltaZ;
            }
        }
        return Optional.empty();
    }

    /**
     * Entry distance of the ray against the axis-aligned box
     * {@code [min, max]} (the entity's bounding box, optionally expanded).
     * {@code 0} when the origin is already inside the box; empty when the ray
     * misses it entirely or runs parallel outside it.
     */
    public static Optional<Double> intersectAabb(Vector origin, Vector direction, Vector min, Vector max) {
        Vector dir = direction.clone().normalize();
        if (dir.lengthSquared() <= 1.0e-9) {
            return Optional.empty();
        }
        double tMin = 0.0;
        double tMax = Double.MAX_VALUE;
        for (int axis = 0; axis < 3; axis++) {
            double o = axisComponent(origin, axis);
            double d = axisComponent(dir, axis);
            double lo = axisComponent(min, axis);
            double hi = axisComponent(max, axis);
            if (Math.abs(d) < 1.0e-9) {
                if (o < lo || o > hi) {
                    return Optional.empty(); // parallel and outside the slab
                }
                continue;
            }
            double t1 = (lo - o) / d;
            double t2 = (hi - o) / d;
            if (t1 > t2) {
                double swap = t1;
                t1 = t2;
                t2 = swap;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return Optional.empty();
            }
        }
        return Optional.of(Math.max(0.0, tMin));
    }

    private static double axisComponent(Vector vector, int axis) {
        return switch (axis) {
            case 0 -> vector.getX();
            case 1 -> vector.getY();
            default -> vector.getZ();
        };
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static int step(double axis) {
        if (axis > 0.0) {
            return 1;
        }
        if (axis < 0.0) {
            return -1;
        }
        return 0;
    }

    /** Distance along the ray until the next boundary on this axis. */
    private static double tMax(double start, int voxel, double axis) {
        if (axis == 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        if (axis > 0.0) {
            return (voxel + 1.0 - start) / axis;
        }
        return (start - voxel) / -axis;
    }
}
