package dev.linqfy.bigCasares.modules.bloodmoon;

import dev.linqfy.bigCasares.modules.environment.EnvironmentalVisualQuality;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodMoonSettingsTest {

    @Test
    void validConfigurationLoadsAllProductionDefaults() {
        YamlConfiguration yaml = configured();
        BloodMoonConfigLoadResult result = BloodMoonSettingsLoader.load(yaml);

        assertTrue(result.valid());
        assertTrue(result.settings().enabled());
        assertEquals(1.5, result.settings().mobs().healthMultiplier());
        assertEquals(1.25, result.settings().mobs().movementSpeedMultiplier());
        assertEquals(2.0, result.settings().spawning().multiplier());
        assertEquals(EnvironmentalVisualQuality.MEDIUM, result.settings().visuals().quality());
        assertTrue(result.settings().worlds().contains("world"));
        assertTrue(result.settings().loot().enabled());
        assertEquals(0.15, result.settings().loot().bonusChance());
    }

    @Test
    void dangerousNegativeConfigurationFailsDisabled() {
        YamlConfiguration yaml = configured();
        yaml.set("blood-moon.mobs.health-multiplier", -1.0);
        yaml.set("blood-moon.spawning.max-extra-hostiles-per-player", -20);

        BloodMoonConfigLoadResult result = BloodMoonSettingsLoader.load(yaml);

        assertFalse(result.valid());
        assertFalse(result.settings().enabled());
        assertEquals(0, result.settings().spawning().maxExtraHostilesPerPlayer());
    }

    @Test
    void invalidSchedulingModeFailsSafe() {
        YamlConfiguration yaml = configured();
        yaml.set("blood-moon.scheduling.mode", "RANDOMLY_MAYBE");

        BloodMoonConfigLoadResult result = BloodMoonSettingsLoader.load(yaml);

        assertFalse(result.valid());
        assertFalse(result.settings().enabled());
    }

    private static YamlConfiguration configured() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("blood-moon.enabled", true);
        yaml.set("blood-moon.scheduling.mode", "CHANCE");
        yaml.set("blood-moon.scheduling.chance-per-night", 0.12);
        yaml.set("blood-moon.scheduling.minimum-normal-nights-between-events", 2);
        yaml.set("blood-moon.scheduling.every-nights", 5);
        yaml.set("blood-moon.worlds", java.util.List.of("world"));
        yaml.set("blood-moon.mobs.health-multiplier", 1.5);
        yaml.set("blood-moon.mobs.movement-speed-multiplier", 1.25);
        yaml.set("blood-moon.mobs.damage-multiplier", 1.0);
        yaml.set("blood-moon.spawning.multiplier", 2.0);
        yaml.set("blood-moon.spawning.min-distance-from-player", 24);
        yaml.set("blood-moon.spawning.max-distance-from-player", 56);
        yaml.set("blood-moon.spawning.max-extra-hostiles-per-player", 20);
        yaml.set("blood-moon.spawning.max-extra-hostiles-per-world", 200);
        yaml.set("blood-moon.visuals.enabled", true);
        yaml.set("blood-moon.visuals.quality", "MEDIUM");
        return yaml;
    }
}
