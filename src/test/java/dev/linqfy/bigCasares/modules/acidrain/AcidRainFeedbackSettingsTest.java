package dev.linqfy.bigCasares.modules.acidrain;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcidRainFeedbackSettingsTest {

    @Test
    void perLevelParticlesAreDistinctByLevel() {
        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(new YamlConfiguration());

        assertTrue(result.valid());
        AcidRainParticleSettings particles = result.settings().feedback().particles();
        AcidRainLevelParticleSettings acid = particles.forLevel(AcidRainLevel.ACID);
        AcidRainLevelParticleSettings toxic = particles.forLevel(AcidRainLevel.TOXIC);
        AcidRainLevelParticleSettings chemical = particles.forLevel(AcidRainLevel.CHEMICAL);

        assertEquals("FALLING_WATER", acid.particle());
        assertEquals("SPORE_BLOSSOM_AIR", toxic.particle());
        assertEquals("DRAGON_BREATH", chemical.particle());
        assertTrue(toxic.count() > acid.count());
        assertTrue(chemical.count() > toxic.count());
    }

    @Test
    void perLevelParticleOverridesFallBackToTheBaseValues() {
        AcidRainParticleSettings particles = new AcidRainParticleSettings(
            true, "FALLING_WATER", 4, 20,
            Map.of(AcidRainLevel.TOXIC, new AcidRainLevelParticleSettings("", -1))
        );

        AcidRainLevelParticleSettings toxic = particles.forLevel(AcidRainLevel.TOXIC);
        assertEquals("FALLING_WATER", toxic.particle());
        assertEquals(4, toxic.count());

        AcidRainLevelParticleSettings unconfigured = particles.forLevel(AcidRainLevel.CHEMICAL);
        assertEquals("FALLING_WATER", unconfigured.particle());
        assertEquals(4, unconfigured.count());
    }

    @Test
    void loadsCustomPerLevelParticles() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.particles.levels.toxic.particle", "SCULK_SOUL");
        yaml.set("acid-rain.particles.levels.toxic.count", 22);
        yaml.set("acid-rain.particles.levels.chemical.particle", "EXPLOSION");

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertTrue(result.valid());
        AcidRainLevelParticleSettings toxic = result.settings().feedback().particles().forLevel(AcidRainLevel.TOXIC);
        assertEquals("SCULK_SOUL", toxic.particle());
        assertEquals(22, toxic.count());
        assertEquals("EXPLOSION", result.settings().feedback().particles().forLevel(AcidRainLevel.CHEMICAL).particle());
    }

    @Test
    void bossBarColorFollowsStormStateByDefault() {
        AcidRainBossBarSettings bossBar = AcidRainBossBarSettings.defaults();

        assertEquals("YELLOW", bossBar.colorFor(AcidRainState.WARNING));
        assertEquals("RED", bossBar.colorFor(AcidRainState.ACTIVE));
        assertEquals("GREEN", bossBar.colorFor(AcidRainState.ENDING));
        assertEquals("GREEN", bossBar.colorFor(AcidRainState.INACTIVE));
    }

    @Test
    void loadsCustomBossBarColors() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.bossbar.colors.warning", "GOLD");
        yaml.set("acid-rain.bossbar.colors.active", "PURPLE");

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertTrue(result.valid());
        AcidRainBossBarSettings bossBar = result.settings().feedback().bossBar();
        assertEquals("GOLD", bossBar.colorFor(AcidRainState.WARNING));
        assertEquals("PURPLE", bossBar.colorFor(AcidRainState.ACTIVE));
        assertEquals("GREEN", bossBar.colorFor(AcidRainState.ENDING));
    }

    @Test
    void impactsAreLoadedAndClampedToASmallBound() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("acid-rain.impacts.count", 999);

        AcidRainConfigLoadResult result = AcidRainSettingsLoader.load(yaml);

        assertTrue(result.valid());
        AcidRainImpactSettings impacts = result.settings().feedback().impacts();
        assertTrue(impacts.enabled());
        assertEquals(16, impacts.count());

        yaml.set("acid-rain.impacts.enabled", false);
        result = AcidRainSettingsLoader.load(yaml);
        assertFalse(result.settings().feedback().impacts().enabled());
    }
}
