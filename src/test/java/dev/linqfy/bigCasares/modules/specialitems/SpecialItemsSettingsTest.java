package dev.linqfy.bigCasares.modules.specialitems;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpecialItemsSettingsTest {

    @Test
    void loadsConfirmedDefaults() {
        SpecialItemsSettings settings = SpecialItemsSettings.load(new YamlConfiguration());

        assertEquals(45_000L, settings.trackingMillis());
        assertEquals(60_000L, settings.cooldownMillis());
        assertEquals(20L, settings.trackerUpdateTicks());
        assertEquals(50.0, settings.nukeHeight());
        assertEquals(6, settings.ringCount());
        assertEquals(350, settings.totalTnt());
        assertEquals(5L, settings.ringIntervalTicks());
        assertEquals(3.0, settings.radiusStep());
    }

    @Test
    void rejectsNonPositiveOrImpossibleValues() {
        assertThrows(IllegalArgumentException.class,
            () -> new SpecialItemsSettings(0L, 60_000L, 20L, 50.0, 5, 200, 5L, 2.0));
        assertThrows(IllegalArgumentException.class,
            () -> new SpecialItemsSettings(45_000L, 60_000L, 20L, 50.0, 5, 31, 5L, 2.0));
        assertThrows(IllegalArgumentException.class,
            () -> new SpecialItemsSettings(45_000L, 60_000L, 20L, -1.0, 5, 200, 5L, 2.0));
    }

    @Test
    void acceptsOneCenterPlusMinimumExponentialRingPoints() {
        assertDoesNotThrow(
            () -> new SpecialItemsSettings(45_000L, 60_000L, 20L, 50.0, 5, 32, 5L, 2.0));
    }
}
