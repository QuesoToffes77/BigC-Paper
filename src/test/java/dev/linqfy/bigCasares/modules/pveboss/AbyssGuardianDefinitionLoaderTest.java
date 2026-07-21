package dev.linqfy.bigCasares.modules.pveboss;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AbyssGuardianDefinitionLoaderTest {

    @Test
    void loadsTheFirstCompleteBossFromYaml() {
        var stream = getClass().getClassLoader().getResourceAsStream("bosses/abyss-guardian.yml");
        var yaml = YamlConfiguration.loadConfiguration(
            new InputStreamReader(stream, StandardCharsets.UTF_8));

        AbyssGuardianDefinition definition = new AbyssGuardianDefinitionLoader().load(yaml);

        assertEquals("abyss-guardian", definition.id());
        assertEquals("GUARDIÁN DEL ABISMO", definition.displayName());
        assertEquals(20_000.0, definition.maximumHealth());
        assertEquals(64.0, definition.audienceRadius());
        assertFalse(definition.abilities().isEmpty());
        assertEquals("void-pulse", definition.abilities().getFirst().id());
        assertEquals(3, definition.musicByPhase().size());
        assertEquals("bigcasares_abyss_guardian", definition.javaModelKey());
        assertEquals(20_000.0, definition.maximumHealthFor(20));
    }

    @Test
    void loadsSahurModelAndRaidScalingWithoutChangingBaseDefinitionHealth() {
        var stream = getClass().getClassLoader().getResourceAsStream("bosses/abyss-guardian.yml");
        var yaml = YamlConfiguration.loadConfiguration(
            new InputStreamReader(stream, StandardCharsets.UTF_8));
        yaml.set("id", "tung-tung-sahur");
        yaml.set("presentation.java-model-key", "boss_sahur");
        yaml.set("raid-scaling.enabled", true);
        yaml.set("raid-scaling.base-health", 20_000.0);
        yaml.set("raid-scaling.health-per-player", 4_000.0);
        yaml.set("raid-scaling.minimum-players", 10);
        yaml.set("raid-scaling.maximum-players", 20);

        AbyssGuardianDefinition definition = new AbyssGuardianDefinitionLoader().load(yaml);

        assertEquals("boss_sahur", definition.javaModelKey());
        assertEquals(20_000.0, definition.maximumHealth());
        assertEquals(60_000.0, definition.maximumHealthFor(4));
        assertEquals(80_000.0, definition.maximumHealthFor(15));
        assertEquals(100_000.0, definition.maximumHealthFor(30));
    }
}
