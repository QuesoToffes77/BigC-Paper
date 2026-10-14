package dev.linqfy.bigCasares.modules.cosmicniv;

import java.util.Locale;
import java.util.Optional;

public enum CosmicNivArmorSlot {
    HELMET,
    CHESTPLATE,
    LEGGINGS,
    BOOTS;

    public String configKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<CosmicNivArmorSlot> fromConfigKey(String key) {
        for (CosmicNivArmorSlot slot : values()) {
            if (slot.configKey().equalsIgnoreCase(key)) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }
}
