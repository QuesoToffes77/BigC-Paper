package dev.linqfy.bigCasares.modules.glider;

import java.util.Locale;
import java.util.Optional;

public enum GliderActivationMode {
    SNEAK,
    FALLING;

    public static Optional<GliderActivationMode> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
