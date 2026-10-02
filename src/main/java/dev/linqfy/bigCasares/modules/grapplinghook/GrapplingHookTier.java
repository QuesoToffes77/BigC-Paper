package dev.linqfy.bigCasares.modules.grapplinghook;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * The six Grappling Hook tiers. Range and cooldown are defaults: the module
 * applies per-tier overrides from config on top of these values.
 */
public enum GrapplingHookTier {
    I("grappling_hook_1", 1012, 50.0, 5.00),
    II("grappling_hook_2", 1013, 75.0, 4.25),
    III("grappling_hook_3", 1014, 100.0, 3.50),
    IV("grappling_hook_4", 1015, 125.0, 2.75),
    V("grappling_hook_5", 1016, 150.0, 2.00),
    VI("grappling_hook_6", 1017, 200.0, 1.25);

    private final String catalogId;
    private final int modelData;
    private final double defaultRange;
    private final double defaultCooldownSeconds;

    GrapplingHookTier(String catalogId, int modelData, double defaultRange, double defaultCooldownSeconds) {
        this.catalogId = catalogId;
        this.modelData = modelData;
        this.defaultRange = defaultRange;
        this.defaultCooldownSeconds = defaultCooldownSeconds;
    }

    public String catalogId() {
        return catalogId;
    }

    public int modelData() {
        return modelData;
    }

    public double defaultRange() {
        return defaultRange;
    }

    public double defaultCooldownSeconds() {
        return defaultCooldownSeconds;
    }

    public static Optional<GrapplingHookTier> fromCatalogId(String catalogId) {
        if (catalogId == null) {
            return Optional.empty();
        }
        String normalized = catalogId.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
            .filter(tier -> tier.catalogId.equals(normalized))
            .findFirst();
    }

    /** Roman numeral, e.g. {@code I}..{@code VI}. */
    public String romanNumeral() {
        return name();
    }
}
