package dev.linqfy.bigCasares.modules.glider;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum GliderTier {
    I(1, "glider_tier_1", 1018),
    II(2, "glider_tier_2", 1019),
    III(3, "glider_tier_3", 1020),
    IV(4, "glider_tier_4", 1021),
    V(5, "glider_tier_5", 1022),
    VI(6, "glider_tier_6", 1023);

    private final int number;
    private final String catalogId;
    private final int modelData;

    GliderTier(int number, String catalogId, int modelData) {
        this.number = number;
        this.catalogId = catalogId;
        this.modelData = modelData;
    }

    public int number() {
        return number;
    }

    public String catalogId() {
        return catalogId;
    }

    public int modelData() {
        return modelData;
    }

    public static Optional<GliderTier> fromCatalogId(String catalogId) {
        if (catalogId == null) {
            return Optional.empty();
        }
        String normalized = catalogId.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(tier -> tier.catalogId.equals(normalized)).findFirst();
    }

    public static Optional<GliderTier> fromNumber(int number) {
        return Arrays.stream(values()).filter(tier -> tier.number == number).findFirst();
    }
}
