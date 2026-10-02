package dev.linqfy.bigCasares.modules.grapplinghook;

import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry for a grapple shot: travel duration, linear interpolation,
 * chain sampling, target selection and the pull impulse. All methods are
 * deterministic and unit-testable without a server.
 */
public final class GrappleShotMath {

    public static final int MAX_CHAIN_SEGMENTS = 96;

    private GrappleShotMath() {
    }

    /** Ticks for the hook to cross {@code distance} at {@code speed} blocks/tick. */
    public static int travelTicks(double distance, double speed) {
        if (distance <= 0.0 || speed <= 0.0) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(distance / speed));
    }

    /** Linear interpolation between two points; {@code t} in {@code [0, 1]}. */
    public static Vector lerp(Vector from, Vector to, double t) {
        double clamped = Math.max(0.0, Math.min(1.0, t));
        return from.clone().multiply(1.0 - clamped).add(to.clone().multiply(clamped));
    }

    /**
     * Positions of the chain segments between {@code from} and {@code to}.
     * Segments are spaced every {@code spacing} blocks starting at
     * {@code spacing} from the origin; the last segment always sits on
     * {@code to}. The list grows monotonically as the hook advances, which is
     * what makes the chain look like it is extending. Long chains increase
     * spacing automatically to stay within the display entity budget.
     */
    public static List<Vector> chainPositions(Vector from, Vector to, double spacing) {
        if (!Double.isFinite(spacing) || spacing <= 0.0) {
            throw new IllegalArgumentException("spacing must be finite and positive");
        }
        double distance = from.distance(to);
        if (!Double.isFinite(distance)) {
            throw new IllegalArgumentException("chain endpoints must be finite");
        }
        double effectiveSpacing = Math.max(spacing, distance / MAX_CHAIN_SEGMENTS);
        int count = Math.min(MAX_CHAIN_SEGMENTS, (int) Math.ceil(distance / effectiveSpacing));
        List<Vector> positions = new ArrayList<>(count);
        for (int index = 1; index <= count; index++) {
            double fraction = index == count ? 1.0 : Math.min(1.0, (index * effectiveSpacing) / distance);
            positions.add(lerp(from, to, fraction));
        }
        return positions;
    }

    /** Centers a vanilla Y-axis chain block on a segment, relative to its midpoint. */
    public static Matrix4f chainTransform(Vector from, Vector to) {
        Vector direction = to.clone().subtract(from);
        double length = direction.length();
        if (!Double.isFinite(length) || length <= 1.0e-9) {
            throw new IllegalArgumentException("chain segment must have finite positive length");
        }
        direction.multiply(1.0 / length);
        Quaternionf rotation = new Quaternionf().rotationTo(
            0.0f, 1.0f, 0.0f,
            (float) direction.getX(), (float) direction.getY(), (float) direction.getZ());
        return new Matrix4f().rotate(rotation).scale(1.0f, (float) length, 1.0f)
            .translate(-0.5f, -0.5f, -0.5f);
    }

    /**
     * Point reached when a shot travels {@code range} blocks along
     * {@code direction} from {@code start} with no hit. Used as the end of
     * the trajectory for a miss so the hook (and chain) still fly exactly
     * along the look direction.
     */
    public static Vector endOfRange(Vector start, Vector direction, double range) {
        Vector normalized = direction.clone();
        if (normalized.lengthSquared() <= 1.0e-9) {
            return start.clone();
        }
        return start.clone().add(normalized.normalize().multiply(Math.max(0.0, range)));
    }

    /**
     * Which target wins a shot: the nearest valid hit. Blocks and entities are
     * measured by the distance along the ray from the eye; the closest one is
     * the target, so an entity behind a wall can never be grabbed (the wall is
     * closer) and an entity in front of a wall wins over it. Distances that
     * are missing or outside the tier range must be passed as
     * {@code Double.MAX_VALUE}.
     */
    public static HitWinner nearestTarget(boolean blockHit, double blockDistance,
                                          boolean entityHit, double entityDistance) {
        boolean hasBlock = blockHit && isValidDistance(blockDistance);
        boolean hasEntity = entityHit && isValidDistance(entityDistance);
        if (!hasBlock && !hasEntity) {
            return HitWinner.NONE;
        }
        if (!hasBlock) {
            return HitWinner.ENTITY;
        }
        if (!hasEntity) {
            return HitWinner.BLOCK;
        }
        return entityDistance <= blockDistance ? HitWinner.ENTITY : HitWinner.BLOCK;
    }

    /**
     * A distance counts as a hit only when it is finite, non-negative and not
     * the {@code Double.MAX_VALUE} sentinel used for "no result" (out of
     * range or not hit at all).
     */
    private static boolean isValidDistance(double distance) {
        return distance >= 0.0 && distance < Double.MAX_VALUE && Double.isFinite(distance);
    }

    /** Which target a shot actually attached to. */
    public enum HitWinner {
        /** No valid target in range: the hook flies to the end of the range. */
        NONE,
        BLOCK,
        ENTITY
    }

    /**
     * Pull impulse: direction from the player toward the anchor (the full
     * target - player vector, normalized), scaled by {@code power}, plus a
     * small {@code upwardBias} on the Y axis so the swing keeps a natural
     * arc. The final magnitude is clamped to {@code maxSpeed} so a
     * misconfigured power can never launch the player at an absurd velocity.
     * Zero-length shots produce a straight-up bias.
     */
    public static Vector impulseVelocity(Vector from, Vector to, double power, double upwardBias, double maxSpeed) {
        Vector direction = to.clone().subtract(from);
        if (direction.lengthSquared() <= 1.0e-9) {
            direction = new Vector(0.0, 1.0, 0.0);
        } else {
            direction.normalize();
        }
        Vector velocity = direction.multiply(power);
        velocity.setY(velocity.getY() + upwardBias);
        double speed = velocity.length();
        if (maxSpeed > 0.0 && speed > maxSpeed) {
            velocity.multiply(maxSpeed / speed);
        }
        return velocity;
    }

    /**
     * Sustained winch pull: a controlled velocity straight toward the target
     * at {@code power} blocks/tick, applied every attach tick while the chain
     * is tense. Returns zero when the puller is already on the target, so a
     * close hook never re-applies an absurd speed. Used for both pulling the
     * player to a block anchor and attracting a hooked entity to the player.
     */
    public static Vector sustainVelocity(Vector from, Vector to, double power) {
        Vector direction = to.clone().subtract(from);
        if (direction.lengthSquared() <= 1.0e-9) {
            return new Vector(0.0, 0.0, 0.0);
        }
        return direction.normalize().multiply(Math.max(0.0, power));
    }
}
