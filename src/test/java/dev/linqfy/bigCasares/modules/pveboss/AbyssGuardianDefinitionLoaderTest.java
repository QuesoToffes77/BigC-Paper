package dev.linqfy.bigCasares.modules.pveboss;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
    }
}
