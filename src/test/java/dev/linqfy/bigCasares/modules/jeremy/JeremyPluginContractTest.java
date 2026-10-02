package dev.linqfy.bigCasares.modules.jeremy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeremyPluginContractTest {

    @Test
    void productionConfigurationUsesSevenMinuteHuntAndOneRealHourRest() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
        JeremyConfigLoadResult result = JeremySettingsLoader.load(yaml);

        assertTrue(result.valid(), () -> String.join(", ", result.errors()));
        assertEquals(420_000L, result.settings().timing().huntMillis());
        assertEquals(3_600_000L, result.settings().timing().restMillis());
        assertEquals(0.345, result.settings().movement().effectiveSpeed(), 0.00001);
    }

    @Test
    void pluginDeclaresCommandPermissionsAndModuleRegistration() throws Exception {
        String pluginYml = Files.readString(Path.of("src/main/resources/plugin.yml"));
        String main = Files.readString(Path.of("src/main/java/dev/linqfy/bigCasares/BigCasares.java"));

        assertTrue(pluginYml.contains("jeremy:"));
        assertTrue(pluginYml.contains("bigcasares.jeremy.admin:"));
        assertTrue(pluginYml.contains("bigcasares.jeremy.start:"));
        assertTrue(pluginYml.contains("bigcasares.jeremy.stop:"));
        assertTrue(main.contains("new JeremyModule"));
        assertTrue(main.contains("moduleManager.register(jeremyModule)"));
    }

    @Test
    void exposesStableModuleId() {
        assertEquals("jeremy", new JeremyModule(null).getId());
    }
}
