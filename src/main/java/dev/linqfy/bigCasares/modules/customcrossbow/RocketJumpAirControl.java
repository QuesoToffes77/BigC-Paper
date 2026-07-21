package dev.linqfy.bigCasares.modules.customcrossbow;

public final class RocketJumpAirControl {
    public static final double ACCELERATION = 0.217;
    public static final double MAX_HORIZONTAL_SPEED = 2.17;

    private RocketJumpAirControl() {
    }

    public static Motion apply(
        double currentX,
        double currentZ,
        float yaw,
        boolean forward,
        boolean backward,
        boolean left,
        boolean right
    ) {
        double forwardInput = (forward ? 1.0 : 0.0) - (backward ? 1.0 : 0.0);
        double rightInput = (right ? 1.0 : 0.0) - (left ? 1.0 : 0.0);
        if (forwardInput == 0.0 && rightInput == 0.0) {
            return new Motion(currentX, currentZ);
        }

        double radians = Math.toRadians(yaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        double rightX = -forwardZ;
        double rightZ = forwardX;
        double desiredX = forwardX * forwardInput + rightX * rightInput;
        double desiredZ = forwardZ * forwardInput + rightZ * rightInput;
        double desiredLength = Math.hypot(desiredX, desiredZ);
        desiredX /= desiredLength;
        desiredZ /= desiredLength;

        double resultX = currentX + desiredX * ACCELERATION;
        double resultZ = currentZ + desiredZ * ACCELERATION;
        double speed = Math.hypot(resultX, resultZ);
        if (speed > MAX_HORIZONTAL_SPEED) {
            double scale = MAX_HORIZONTAL_SPEED / speed;
            resultX *= scale;
            resultZ *= scale;
        }
        return new Motion(resultX, resultZ);
    }

    public record Motion(double x, double z) {
    }
}
