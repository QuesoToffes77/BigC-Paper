package dev.linqfy.bigCasares.modules.bloodmoon;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodMoonPluginContractTest {

    @Test
    void pluginDeclaresCommandPermissionsAndModuleRegistration() throws Exception {
        String pluginYml = Files.readString(Path.of("src/main/resources/plugin.yml"));
        String main = Files.readString(Path.of(
            "src/main/java/dev/linqfy/bigCasares/BigCasares.java"));

        assertTrue(pluginYml.contains("bloodmoon:"));
        assertTrue(pluginYml.contains("bigcasares.bloodmoon.admin:"));
        assertTrue(pluginYml.contains("bigcasares.bloodmoon.start:"));
        assertTrue(pluginYml.contains("bigcasares.bloodmoon.stop:"));
        assertTrue(main.contains("new BloodMoonModule"));
        assertTrue(main.contains("moduleManager.register(bloodMoonModule)"));
    }

    @Test
    void productionConfigurationLoadsEnabledAndSafe() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
            new File("src/main/resources/config.yml"));

        BloodMoonConfigLoadResult result = BloodMoonSettingsLoader.load(yaml);

        assertTrue(result.valid(), () -> String.join(", ", result.errors()));
        assertTrue(result.settings().enabled());
        assertEquals(1.5, result.settings().mobs().healthMultiplier());
        assertEquals(1.25, result.settings().mobs().movementSpeedMultiplier());
        assertEquals(1.0, result.settings().mobs().damageMultiplier());
        assertFalse(result.settings().allowWithOtherEvents());
    }
}
