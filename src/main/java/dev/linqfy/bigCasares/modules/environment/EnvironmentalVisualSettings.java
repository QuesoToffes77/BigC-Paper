package dev.linqfy.bigCasares.modules.environment;

public record EnvironmentalVisualSettings(
    boolean enabled,
    EnvironmentalVisualQuality quality,
    double horizontalRadius,
    double verticalRadius,
    int intervalTicks
) {
    public EnvironmentalVisualSettings {
        quality = quality == null ? EnvironmentalVisualQuality.MEDIUM : quality;
        horizontalRadius = clamp(horizontalRadius, 2.0, 32.0);
        verticalRadius = clamp(verticalRadius, 2.0, 20.0);
        intervalTicks = Math.max(2, Math.min(100, intervalTicks));
    }

    public static EnvironmentalVisualSettings defaults() {
        return new EnvironmentalVisualSettings(true, EnvironmentalVisualQuality.MEDIUM, 14.0, 8.0, 10);
    }

    public static EnvironmentalVisualSettings disabled() {
        return new EnvironmentalVisualSettings(false, EnvironmentalVisualQuality.LOW, 8.0, 5.0, 20);
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }
}
