package dev.linqfy.bigCasares.modules.glider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GliderCommandTest {

    @Test
    void acceptsNumericRomanAndCatalogTierNames() {
        assertEquals(GliderTier.I, GliderCommand.parseTier("1").orElseThrow());
        assertEquals(GliderTier.III, GliderCommand.parseTier("III").orElseThrow());
        assertEquals(GliderTier.VI, GliderCommand.parseTier("tier-6").orElseThrow());
        assertEquals(GliderTier.IV, GliderCommand.parseTier("glider_tier_4").orElseThrow());
        assertTrue(GliderCommand.parseTier("7").isEmpty());
    }

    @Test
    void pluginDeclaresDedicatedGliderCommandAndPermissions() throws Exception {
        String pluginYml = java.nio.file.Files.readString(java.nio.file.Path.of(
            "src", "main", "resources", "plugin.yml"));
        String module = java.nio.file.Files.readString(java.nio.file.Path.of(
            "src", "main", "java", "dev", "linqfy", "bigCasares", "modules", "glider", "GliderModule.java"));

        assertTrue(pluginYml.contains("  glider:"));
        assertTrue(pluginYml.contains("bigcasares.glider.admin:"));
        assertTrue(pluginYml.contains("bigcasares.glider.give:"));
        assertTrue(module.contains("bindCommand(\"glider-command\""));
    }
}
