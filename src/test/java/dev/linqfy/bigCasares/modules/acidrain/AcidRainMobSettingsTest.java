package dev.linqfy.bigCasares.modules.acidrain;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcidRainMobSettingsTest {

    @Test
    void defaultsAreEnabledWithBoundedValues() {
        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(new YamlConfiguration());

        assertTrue(result.valid());
        AcidRainMobSettings mobs = result.settings().mobs();
        assertTrue(mobs.enabled());
        assertEquals(60, mobs.spawnIntervalTicks());
        assertEquals(2, mobs.attemptsPerCycle());
        assertEquals(20, mobs.maxActive());
        assertEquals(24, mobs.radius());
        assertEquals(3, mobs.enabledTypes().size());
        assertTrue(mobs.canSpawn());
    }

    @Test
    void loadsCustomMobValuesAndTypeToggles() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.mobs.enabled", true);
        yaml.set("acid-rain.mobs.spawn.interval-ticks", 80);
        yaml.set("acid-rain.mobs.spawn.attempts-per-cycle", 3);
        yaml.set("acid-rain.mobs.spawn.max-active", 12);
        yaml.set("acid-rain.mobs.spawn.radius", 30);
        yaml.set("acid-rain.mobs.types.crawler", true);
        yaml.set("acid-rain.mobs.types.brute", false);

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertTrue(result.valid());
        AcidRainMobSettings mobs = result.settings().mobs();
        assertEquals(80, mobs.spawnIntervalTicks());
        assertEquals(3, mobs.attemptsPerCycle());
        assertEquals(12, mobs.maxActive());
        assertEquals(30, mobs.radius());
        assertTrue(mobs.isTypeEnabled(AcidRainMobType.CRAWLER));
        assertFalse(mobs.isTypeEnabled(AcidRainMobType.BRUTE));
        assertTrue(mobs.isTypeEnabled(AcidRainMobType.SPITTER));
    }

    @Test
    void negativeMobValuesFailSafe() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.mobs.spawn.max-active", -5);

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertFalse(result.valid());
        assertFalse(result.settings().mobs().enabled());
        assertFalse(result.settings().damage().enabled());
        assertFalse(result.errors().isEmpty());
    }

    @Test
    void dropSettingsHaveBoundedDefaults() {
        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(new YamlConfiguration());

        assertTrue(result.valid());
        AcidRainMobDropSettings drops = result.settings().mobs().drops();
        assertTrue(drops.enabled());
        assertEquals(100, drops.chancePercent());
        assertEquals(1, drops.minAmount());
        assertEquals(2, drops.maxAmount());
    }

    @Test
    void loadsCustomDropSettings() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.mobs.drops.enabled", false);
        yaml.set("acid-rain.mobs.drops.chance-percent", 40);
        yaml.set("acid-rain.mobs.drops.min-amount", 1);
        yaml.set("acid-rain.mobs.drops.max-amount", 4);

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertTrue(result.valid());
        AcidRainMobDropSettings drops = result.settings().mobs().drops();
        assertFalse(drops.enabled());
        assertEquals(40, drops.chancePercent());
        assertEquals(1, drops.minAmount());
        assertEquals(4, drops.maxAmount());
    }

    @Test
    void absurdDropValuesAreClamped() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.mobs.drops.chance-percent", 999);
        yaml.set("acid-rain.mobs.drops.min-amount", -3);
        yaml.set("acid-rain.mobs.drops.max-amount", 99);

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertTrue(result.valid());
        AcidRainMobDropSettings drops = result.settings().mobs().drops();
        assertEquals(100, drops.chancePercent());
        assertEquals(0, drops.minAmount());
        assertEquals(99, drops.maxAmount());
    }

    @Test
    void absurdMobValuesAreClamped() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.mobs.spawn.interval-ticks", 99999);
        yaml.set("acid-rain.mobs.spawn.attempts-per-cycle", 999);
        yaml.set("acid-rain.mobs.spawn.max-active", 99999);
        yaml.set("acid-rain.mobs.spawn.radius", 99999);

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertTrue(result.valid());
        AcidRainMobSettings mobs = result.settings().mobs();
        assertEquals(600, mobs.spawnIntervalTicks());
        assertEquals(32, mobs.attemptsPerCycle());
        assertEquals(200, mobs.maxActive());
        assertEquals(64, mobs.radius());
        assertFalse(result.warnings().isEmpty());
    }

    @Test
    void disabledSettingsCannotSpawn() {
        assertFalse(AcidRainMobSettings.disabled().canSpawn());
    }

    @Test
    void allTypesDisabledCannotSpawn() {
        AcidRainMobSettings allOff = new AcidRainMobSettings(true, 60, 2, 20, 24,
            Map.of(AcidRainMobType.CRAWLER, false, AcidRainMobType.BRUTE, false, AcidRainMobType.SPITTER, false));

        assertFalse(allOff.canSpawn());
        assertTrue(allOff.enabledTypes().isEmpty());
    }

    @Test
    void spawnEffectAndAmbientParticlesHaveBoundedDefaults() {
        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(new YamlConfiguration());

        assertTrue(result.valid());
        AcidRainMobSpawnEffectSettings spawnEffect = result.settings().mobs().spawnEffect();
        assertTrue(spawnEffect.enabled());
        assertEquals(8, spawnEffect.particles());
        assertTrue(spawnEffect.sound().enabled());
        assertEquals("ENTITY_WITCH_AMBIENT", spawnEffect.sound().sound());

        AcidRainMobAmbientSettings ambient = result.settings().mobs().ambient();
        assertTrue(ambient.enabled());
        assertEquals(20, ambient.intervalTicks());
        assertEquals(2, ambient.count());
        assertEquals("SPORE_BLOSSOM_AIR", ambient.particle());
    }

    @Test
    void loadsCustomSpawnEffectAndAmbientParticles() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.mobs.spawn-effect.enabled", false);
        yaml.set("acid-rain.mobs.spawn-effect.particles", 12);
        yaml.set("acid-rain.mobs.spawn-effect.sound.sound", "ENTITY_HUSK_AMBIENT");
        yaml.set("acid-rain.mobs.spawn-effect.sound.volume", 0.4);
        yaml.set("acid-rain.mobs.ambient-particles.interval-ticks", 40);
        yaml.set("acid-rain.mobs.ambient-particles.count", 4);
        yaml.set("acid-rain.mobs.ambient-particles.particle", "SCULK_SOUL");

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertTrue(result.valid());
        AcidRainMobSpawnEffectSettings spawnEffect = result.settings().mobs().spawnEffect();
        assertFalse(spawnEffect.enabled());
        assertEquals(12, spawnEffect.particles());
        assertEquals("ENTITY_HUSK_AMBIENT", spawnEffect.sound().sound());
        assertEquals(0.4f, spawnEffect.sound().volume());

        AcidRainMobAmbientSettings ambient = result.settings().mobs().ambient();
        assertEquals(40, ambient.intervalTicks());
        assertEquals(4, ambient.count());
        assertEquals("SCULK_SOUL", ambient.particle());
    }

    @Test
    void absurdSpawnEffectAndAmbientValuesAreClamped() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.mobs.spawn-effect.particles", 99999);
        yaml.set("acid-rain.mobs.ambient-particles.interval-ticks", 99999);
        yaml.set("acid-rain.mobs.ambient-particles.count", 99999);

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertTrue(result.valid());
        assertEquals(32, result.settings().mobs().spawnEffect().particles());
        assertEquals(200, result.settings().mobs().ambient().intervalTicks());
        assertEquals(16, result.settings().mobs().ambient().count());
    }
}
