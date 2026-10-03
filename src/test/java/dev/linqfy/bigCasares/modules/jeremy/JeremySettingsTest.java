package dev.linqfy.bigCasares.modules.jeremy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeremySettingsTest {

    @Test
    void existingServersWithoutJeremySectionUseSafeDefaults() {
        JeremyConfigLoadResult result = JeremySettingsLoader.load(new YamlConfiguration());

        assertTrue(result.valid());
        assertTrue(result.settings().enabled());
        assertEquals(3_600_000L, result.settings().timing().restMillis());
        assertFalse(result.warnings().isEmpty());
    }

    @Test
    void productionDefaultsAreSafeAndComplete() {
        JeremyConfigLoadResult result = JeremySettingsLoader.load(
            YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml")));

        assertTrue(result.valid(), () -> String.join(", ", result.errors()));
        assertEquals(24, result.settings().spawn().minDistance());
        assertEquals(40, result.settings().spawn().maxDistance());
        assertEquals(32.0, result.settings().ultrasound().range());
        assertEquals(2, result.settings().ultrasound().wallPenetration().maxBlocks());
        assertFalse(result.settings().targeting().creative());
        assertFalse(result.settings().targeting().spectator());
        assertTrue(result.settings().loot().enabled());
        assertEquals(75, result.settings().loot().experience());
    }

    @Test
    void invalidDangerousValuesDisableTheModuleFailSafe() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("jeremy.enabled", true);
        yaml.set("jeremy.timing.hunt-seconds", -1);
        yaml.set("jeremy.timing.rest-seconds", -1);
        yaml.set("jeremy.spawn.min-distance", -100);
        yaml.set("jeremy.ultrasound.wall-penetration.max-blocks", 1000);

        JeremyConfigLoadResult result = JeremySettingsLoader.load(yaml);

        assertFalse(result.valid());
        assertFalse(result.settings().enabled());
        assertFalse(result.settings().ultrasound().enabled());
    }

    @Test
    void jeremyUsesConfiguredBabyZombieEquivalentSpeed() {
        assertEquals(0.345, JeremyMovementSettings.defaults().effectiveSpeed(), 0.00001);
        assertEquals(0.40, new JeremyMovementSettings(false, 0.40).effectiveSpeed(), 0.00001);
    }
}
