package dev.linqfy.bigCasares.modules.glider;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GliderSettingsTest {

    @Test
    void tierStatsAreLoaded() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("glider.tiers.3.forward-speed", 0.53);
        yaml.set("glider.tiers.3.boost.power", 0.48);
        yaml.set("glider.tiers.3.boost.cooldown", 78);

        GliderSettings settings = GliderSettings.load(yaml, ignored -> { });

        assertEquals(0.53, settings.stats(GliderTier.III).forwardSpeed());
        assertEquals(0.48, settings.stats(GliderTier.III).boostPower());
        assertEquals(78, settings.stats(GliderTier.III).boostCooldownTicks());
    }

    @Test
    void boostUnlocksAtTierThree() {
        GliderSettings settings = GliderSettings.defaults();

        assertFalse(settings.stats(GliderTier.I).boostEnabled());
        assertFalse(settings.stats(GliderTier.II).boostEnabled());
        assertTrue(settings.stats(GliderTier.III).boostEnabled());
        assertTrue(settings.stats(GliderTier.VI).boostEnabled());
    }

    @Test
    void invalidProgressionFallsBackToSafeDefaults() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("glider.tiers.2.forward-speed", 0.10);
        List<String> warnings = new ArrayList<>();

        GliderSettings settings = GliderSettings.load(yaml, warnings::add);

        assertEquals(0.44, settings.stats(GliderTier.II).forwardSpeed());
        assertFalse(warnings.isEmpty());
    }

    @Test
    void deployedVisualSettingsAreLoadedAndClamped() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("glider.visual.vertical-offset", 2.8);
        yaml.set("glider.visual.scale", 20.0);
        yaml.set("glider.visual.teleport-duration-ticks", 200);

        GliderVisualSettings visual = GliderSettings.load(yaml, ignored -> { }).visuals();

        assertTrue(visual.enabled());
        assertEquals(2.8, visual.verticalOffset());
        assertEquals(4.0, visual.scale());
        assertEquals(59, visual.teleportDurationTicks());
    }
}
