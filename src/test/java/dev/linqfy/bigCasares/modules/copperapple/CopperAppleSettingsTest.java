package dev.linqfy.bigCasares.modules.copperapple;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CopperAppleSettingsTest {

    @Test
    void loadsProductionDefaults() {
        CopperAppleSettings settings = CopperAppleSettings.load(new YamlConfiguration());

        assertEquals(true, settings.oxidationEnabled());
        assertEquals(64, settings.maxStackSize());
        assertEquals(5_000L, settings.consumeCooldownMillis());
        assertEquals(100L, settings.scanIntervalTicks());
        assertEquals(900_000L, settings.oxidationPolicy().exposedAfterMillis());
        assertEquals(1_800_000L, settings.oxidationPolicy().weatheredAfterMillis());
        assertEquals(3_600_000L, settings.oxidationPolicy().oxidizedAfterMillis());
    }

    @Test
    void normalizesUnsafeOrUnorderedValues() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("copper-apple.consume-cooldown-ms", -50L);
        config.set("copper-apple.oxidation.scan-interval-ticks", 1L);
        config.set("copper-apple.oxidation.exposed-after-seconds", 900L);
        config.set("copper-apple.oxidation.weathered-after-seconds", 300L);
        config.set("copper-apple.oxidation.oxidized-after-seconds", -1L);
        config.set("copper-apple.max-stack-size", -20);

        CopperAppleSettings settings = CopperAppleSettings.load(config);

        assertEquals(0L, settings.consumeCooldownMillis());
        assertEquals(64, settings.maxStackSize());
        assertEquals(20L, settings.scanIntervalTicks());
        assertEquals(900_000L, settings.oxidationPolicy().exposedAfterMillis());
        assertEquals(900_000L, settings.oxidationPolicy().weatheredAfterMillis());
        assertEquals(3_600_000L, settings.oxidationPolicy().oxidizedAfterMillis());
    }
}
