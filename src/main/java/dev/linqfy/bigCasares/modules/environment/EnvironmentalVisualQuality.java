package dev.linqfy.bigCasares.modules.environment;

import java.util.Locale;
import java.util.Optional;

public enum EnvironmentalVisualQuality {
    LOW(0.55),
    MEDIUM(1.0),
    HIGH(1.45);

    private final double densityMultiplier;

    EnvironmentalVisualQuality(double densityMultiplier) {
        this.densityMultiplier = densityMultiplier;
    }

    double densityMultiplier() {
        return densityMultiplier;
    }

    public static Optional<EnvironmentalVisualQuality> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
