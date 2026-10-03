package dev.linqfy.bigCasares.modules.glider;

import org.bukkit.util.Vector;

public final class GliderPhysics {

    public static final double MAX_GLIDE_HORIZONTAL_SPEED = 1.35;
    public static final double MAX_BOOST_HORIZONTAL_SPEED = 1.55;
    public static final double MAX_BOOST_VERTICAL_SPEED = 0.12;
    private static final double MINIMUM_DESCENT = -0.025;

    private GliderPhysics() {
    }

    public static Vector step(Vector currentVelocity, Vector lookDirection, GliderTierStats stats) {
        Vector current = safeVector(currentVelocity);
        Vector look = normalizedLook(lookDirection);
        Vector horizontalLook = new Vector(look.getX(), 0.0, look.getZ());
        if (horizontalLook.lengthSquared() < 0.000001) {
            horizontalLook = new Vector(current.getX(), 0.0, current.getZ());
        }
        if (horizontalLook.lengthSquared() > 0.000001) {
            horizontalLook.normalize();
        }

        double climb = clamp(look.getY(), 0.0, 0.65);
        double dive = clamp(-look.getY(), 0.0, 0.85);
        double descentEnergy = clamp((-current.getY()) - (-stats.maxFallSpeed()), 0.0, 0.8);
        double speedMultiplier = 1.0 + dive * 0.25 + descentEnergy * 0.18 - climb * 0.28;
        double desiredSpeed = Math.max(0.12, stats.forwardSpeed() * speedMultiplier);
        Vector desiredHorizontal = horizontalLook.multiply(desiredSpeed);
        Vector horizontal = new Vector(current.getX(), 0.0, current.getZ())
            .multiply(1.0 - stats.steering())
            .add(desiredHorizontal.multiply(stats.steering()));
        limitHorizontal(horizontal, MAX_GLIDE_HORIZONTAL_SPEED);

        double vertical;
        if (current.getY() > 0.0) {
            vertical = current.getY() * 0.55 - 0.04;
            if (vertical < 0.0) {
                vertical = MINIMUM_DESCENT;
            }
        } else {
            vertical = Math.max(current.getY(), stats.maxFallSpeed());
            vertical += stats.lift() * climb;
            vertical -= 0.06 * dive;
            vertical = Math.max(vertical, stats.maxFallSpeed() * 1.35);
            vertical = Math.min(vertical, MINIMUM_DESCENT);
        }
        vertical = Math.min(vertical, MAX_BOOST_VERTICAL_SPEED);
        return new Vector(horizontal.getX(), vertical, horizontal.getZ());
    }

    public static Vector boost(Vector currentVelocity, Vector lookDirection, GliderTierStats stats) {
        Vector current = safeVector(currentVelocity);
        Vector look = normalizedLook(lookDirection);
        Vector horizontalLook = new Vector(look.getX(), 0.0, look.getZ());
        if (horizontalLook.lengthSquared() > 0.000001) {
            horizontalLook.normalize();
        }
        Vector boosted = current.clone().add(horizontalLook.multiply(stats.boostPower()));
        double verticalImpulse = clamp(look.getY() * stats.boostPower() * 0.35, -0.12, 0.12);
        boosted.setY(clamp(boosted.getY() + verticalImpulse, -0.65, MAX_BOOST_VERTICAL_SPEED));
        limitHorizontal(boosted, MAX_BOOST_HORIZONTAL_SPEED);
        return boosted;
    }

    private static Vector normalizedLook(Vector source) {
        Vector look = safeVector(source);
        if (look.lengthSquared() < 0.000001) {
            return new Vector(0.0, 0.0, 1.0);
        }
        return look.normalize();
    }

    private static Vector safeVector(Vector source) {
        if (source == null
            || !Double.isFinite(source.getX())
            || !Double.isFinite(source.getY())
            || !Double.isFinite(source.getZ())) {
            return new Vector();
        }
        return source.clone();
    }

    private static void limitHorizontal(Vector vector, double maximum) {
        double length = Math.hypot(vector.getX(), vector.getZ());
        if (length > maximum) {
            double scale = maximum / length;
            vector.setX(vector.getX() * scale);
            vector.setZ(vector.getZ() * scale);
        }
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }
}
