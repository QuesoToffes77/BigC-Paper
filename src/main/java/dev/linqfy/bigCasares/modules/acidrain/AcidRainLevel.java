package dev.linqfy.bigCasares.modules.acidrain;

import java.util.Locale;
import java.util.Optional;

public enum AcidRainLevel {
    ACID,
    TOXIC,
    CHEMICAL;

    public static Optional<AcidRainLevel> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException failure) {
            return Optional.empty();
        }
    }
}
