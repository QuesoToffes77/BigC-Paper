package dev.linqfy.bigCasares.modules.acidrain;

import java.util.Locale;
import java.util.Optional;

/**
 * The Acid Rain mob family. Purely the identity of each type: its display
 * label and its relative spawn weight. Bukkit mapping (vanilla entity bases,
 * attributes, names) lives in {@link BukkitAcidRainMobFactory}.
 */
public enum AcidRainMobType {
    CRAWLER("Crawler", 5),
    BRUTE("Brute", 2),
    SPITTER("Spitter", 2);

    private final String label;
    private final int weight;

    AcidRainMobType(String label, int weight) {
        this.label = label;
        this.weight = weight;
    }

    public String label() {
        return label;
    }

    public int weight() {
        return weight;
    }

    public static Optional<AcidRainMobType> parse(String raw) {
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
