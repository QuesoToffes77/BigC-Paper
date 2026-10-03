package dev.linqfy.bigCasares.modules.grapplinghook;

import org.bukkit.util.Vector;

/** Pure movement math for controllable rope swinging. */
public final class GrappleMovement {

    private GrappleMovement() {
    }

    public static Vector controlledVelocity(
        Vector current,
        Vector reel,
        float yaw,
        boolean forward,
        boolean backward,
        boolean left,
        boolean right,
        boolean jump,
        double acceleration,
        double maxHorizontalSpeed,
        double jumpVelocity
    ) {
        Vector result = current == null ? new Vector() : current.clone();
        Vector reelVelocity = reel == null ? new Vector() : reel.clone();

        double reelSpeed = reelVelocity.length();
        if (reelSpeed > 1.0e-9) {
            Vector ropeDirection = reelVelocity.clone().multiply(1.0 / reelSpeed);
            double currentTowardAnchor = result.dot(ropeDirection);
            if (currentTowardAnchor < reelSpeed) {
                result.add(ropeDirection.multiply(reelSpeed - currentTowardAnchor));
            }
        }

        double forwardInput = (forward ? 1.0 : 0.0) - (backward ? 1.0 : 0.0);
        double rightInput = (right ? 1.0 : 0.0) - (left ? 1.0 : 0.0);
        if (forwardInput != 0.0 || rightInput != 0.0) {
            double radians = Math.toRadians(yaw);
            double forwardX = -Math.sin(radians);
            double forwardZ = Math.cos(radians);
            double rightX = -forwardZ;
            double rightZ = forwardX;
            double inputX = forwardX * forwardInput + rightX * rightInput;
            double inputZ = forwardZ * forwardInput + rightZ * rightInput;
            double inputLength = Math.hypot(inputX, inputZ);
            result.add(new Vector(
                inputX / inputLength * Math.max(0.0, acceleration),
                0.0,
                inputZ / inputLength * Math.max(0.0, acceleration)
            ));
        }

        double reelHorizontal = Math.hypot(reelVelocity.getX(), reelVelocity.getZ());
        double horizontalLimit = reelHorizontal + Math.max(0.0, maxHorizontalSpeed);
        double horizontalSpeed = Math.hypot(result.getX(), result.getZ());
        if (horizontalLimit > 0.0 && horizontalSpeed > horizontalLimit) {
            double scale = horizontalLimit / horizontalSpeed;
            result.setX(result.getX() * scale);
            result.setZ(result.getZ() * scale);
        }

        if (jump && jumpVelocity > 0.0) {
            result.setY(Math.min(jumpVelocity * 2.0, Math.max(0.0, result.getY()) + jumpVelocity));
        }

        double speed = result.length();
        if (speed > GrapplingHookSettings.MAX_IMPULSE_SPEED) {
            result.multiply(GrapplingHookSettings.MAX_IMPULSE_SPEED / speed);
        }
        return result;
    }
}
