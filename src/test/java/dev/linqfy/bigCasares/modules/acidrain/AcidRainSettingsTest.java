package dev.linqfy.bigCasares.modules.acidrain;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcidRainSettingsTest {

    @Test
    void loadsValidConfiguration() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.duration.minimum-seconds", 300);
        yaml.set("acid-rain.duration.maximum-seconds", 900);
        yaml.set("acid-rain.worlds.enabled", java.util.List.of("world", "event"));
        yaml.set("acid-rain.environment.destruction.whitelist", java.util.List.of("STONE", "COBBLESTONE"));

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertTrue(result.valid());
        assertEquals(300, result.settings().duration().minimumSeconds());
        assertTrue(result.settings().worlds().isAffected("world"));
        assertTrue(result.settings().environment().destruction().whitelist().contains(Material.STONE));
        assertEquals(16, result.settings().environment().destruction().verticalScanDepth());
    }

    @Test
    void invalidDurationFailsSafeInsteadOfBecomingUnlimited() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.duration.minimum-seconds", -1);
        yaml.set("acid-rain.duration.maximum-seconds", 9999999);

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertFalse(result.valid());
        assertFalse(result.settings().damage().enabled());
        assertFalse(result.settings().environment().destruction().enabled());
        assertFalse(result.errors().isEmpty());
    }

    @Test
    void zeroLimitsMeanNoEnvironmentalDestruction() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.environment.destruction.max-blocks-per-event", 0);
        yaml.set("acid-rain.environment.destruction.max-blocks-per-second", 0);

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertTrue(result.valid());
        assertFalse(result.settings().environment().destruction().canDestroyBlocks());
    }

    @Test
    void emptyWhitelistMeansNoDestructionAndInvalidMaterialsAreIgnored() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.environment.destruction.whitelist", java.util.List.of("NOPE", "STONE"));

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertTrue(result.valid());
        assertEquals(java.util.Set.of(Material.STONE), result.settings().environment().destruction().whitelist());

        yaml.set("acid-rain.environment.destruction.whitelist", java.util.List.of());
        result = AcidRainSettingsLoader.load(yaml);

        assertTrue(result.valid());
        assertTrue(result.settings().environment().destruction().whitelist().isEmpty());
        assertFalse(result.settings().environment().destruction().canDestroyBlocks());
    }

    @Test
    void invalidDefaultLevelIsRejectedSafely() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.default-level", "radiation");

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertFalse(result.valid());
        assertEquals(AcidRainLevel.ACID, result.settings().defaultLevel());
        assertFalse(result.settings().automatic().enabled());
    }

    @Test
    void negativeVerticalScanDepthDisablesDestructionSafely() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.environment.destruction.vertical-scan-depth", -1);

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertFalse(result.valid());
        assertFalse(result.settings().environment().destruction().enabled());
    }

    @Test
    void perLevelDestructionTuningIsLoadedAndDifferentiated() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.levels.acid.destruction.interval-ticks", 40);
        yaml.set("acid-rain.levels.acid.destruction.candidates-per-cycle", 30);
        yaml.set("acid-rain.levels.toxic.destruction.interval-ticks", 20);
        yaml.set("acid-rain.levels.toxic.destruction.candidates-per-cycle", 60);
        yaml.set("acid-rain.levels.chemical.destruction.interval-ticks", 10);
        yaml.set("acid-rain.levels.chemical.destruction.candidates-per-cycle", 90);

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertTrue(result.valid());
        AcidRainLevelSettings acid = result.settings().settingsFor(AcidRainLevel.ACID);
        AcidRainLevelSettings toxic = result.settings().settingsFor(AcidRainLevel.TOXIC);
        AcidRainLevelSettings chemical = result.settings().settingsFor(AcidRainLevel.CHEMICAL);
        assertTrue(acid.destruction().intervalTicks() > toxic.destruction().intervalTicks());
        assertTrue(toxic.destruction().intervalTicks() > chemical.destruction().intervalTicks());
        assertTrue(acid.destruction().candidatesPerCycle() < toxic.destruction().candidatesPerCycle());
        assertTrue(toxic.destruction().candidatesPerCycle() < chemical.destruction().candidatesPerCycle());
    }

    @Test
    void perLevelCandidatesFallBackToTheGlobalCandidatesPerCycle() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.environment.destruction.candidates-per-cycle", 77);

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertTrue(result.valid());
        assertEquals(77, result.settings().settingsFor(AcidRainLevel.TOXIC).destruction().candidatesPerCycle());
    }

    @Test
    void nonFiniteDamageIsRejectedSafely() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.levels.toxic.damage", Double.NaN);
        yaml.set("acid-rain.levels.toxic.contamination-intensity", Double.POSITIVE_INFINITY);

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertFalse(result.valid());
        assertFalse(result.settings().damage().enabled());
        assertFalse(result.settings().environment().destruction().enabled());
    }

    @Test
    void absurdDestructionValuesAreClampedInsteadOfBecomingUnlimited() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.environment.destruction.radius", 100000);
        yaml.set("acid-rain.environment.destruction.max-blocks-per-event", 999999999);
        yaml.set("acid-rain.environment.destruction.max-blocks-per-second", 99999);
        yaml.set("acid-rain.environment.destruction.candidates-per-cycle", 99999);
        yaml.set("acid-rain.environment.destruction.vertical-scan-depth", 10000);

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertTrue(result.valid());
        AcidRainDestructionSettings destruction = result.settings().environment().destruction();
        assertEquals(64, destruction.radius());
        assertEquals(10000, destruction.maxBlocksPerEvent());
        assertEquals(120, destruction.maxBlocksPerSecond());
        assertEquals(512, destruction.candidatesPerCycle());
        assertEquals(128, destruction.verticalScanDepth());
        assertFalse(result.warnings().isEmpty());
    }

    @Test
    void negativePerLevelCandidatesFailSafe() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.levels.toxic.destruction.candidates-per-cycle", -5);

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertFalse(result.valid());
        assertFalse(result.settings().environment().destruction().enabled());
    }
}
