package dev.linqfy.bigCasares.modules.toffy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToffyModuleWiringTest {

    @Test
    void exposesStableModuleId() {
        assertEquals("toffy-complementaries", new ToffyModule(null).getId());
        assertEquals("toffy-complementaries", ToffyModule.MODULE_ID);
    }

    @Test
    void isUnavailableUntilEnabled() {
        ToffyModule module = new ToffyModule(null);
        module.onEnable();

        assertFalse(module.isAvailable());
        assertThrows(IllegalStateException.class, () -> module.evaluate(ToffyLoadout.empty()));
        assertThrows(IllegalStateException.class, module::maximum);
        module.onDisable();
    }

    @Test
    void bothBundledFilesShipInsideTheJar() throws Exception {
        for (String name : new String[] {ToffyModule.CONFIG_FILE, ToffyModule.VALUES_FILE}) {
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(name)) {
                assertNotNull(stream, name);
            }
        }
        assertEquals("toffy_complementaries.yml", ToffyModule.CONFIG_FILE);
        assertEquals("toffy_valores.yml", ToffyModule.VALUES_FILE);
    }

    @Test
    void moduleIsEnabledInTheProductionConfig() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));

        assertTrue(config.getBoolean("modules.toffy-complementaries.enabled", false));
    }

    @Test
    void pluginDeclaresCommandPermissionsAndModuleRegistration() throws Exception {
        YamlConfiguration plugin = new YamlConfiguration();
        plugin.options().pathSeparator('/');
        plugin.load(new File("src/main/resources/plugin.yml"));
        String main = Files.readString(Path.of("src/main/java/dev/linqfy/bigCasares/BigCasares.java"));

        assertTrue(plugin.isConfigurationSection("commands/toffy"));
        assertTrue(plugin.getStringList("commands/toffy/aliases").contains("tcomp"));
        assertEquals("true", plugin.getString("permissions/" + ToffyCommand.CHECK_PERMISSION + "/default"));
        assertEquals("op", plugin.getString("permissions/" + ToffyCommand.CHECK_OTHERS_PERMISSION + "/default"));
        assertEquals("op", plugin.getString("permissions/" + ToffyCommand.ADMIN_PERMISSION + "/default"));
        assertTrue(plugin.getBoolean("permissions/bigcasares.*/children/" + ToffyCommand.ADMIN_PERMISSION, false));
        assertTrue(main.contains("new ToffyModule(this,"));
        assertTrue(main.contains("toffyModule.lastValidSettings()"));
        assertTrue(main.contains("moduleManager.register(toffyModule)"));
        assertTrue(main.contains("public ToffyModule getToffyModule()"));
    }

    @Test
    void theCommandShowsLevelsWithTheConfiguredDecimals() {
        assertEquals("53", ToffyCommand.level(52.5, 0));
        assertEquals("52.5", ToffyCommand.level(52.5, 1));
        assertEquals("52.523", ToffyCommand.level(52.523, 3));
    }

    @Test
    void aNewInstanceStartsFromThePreviousValidSettings() {
        ToffySettings settings = ToffySettingsLoaderTest.bundledSettings();

        assertEquals(settings, new ToffyModule(null, settings).lastValidSettings());
        assertEquals(null, new ToffyModule(null).lastValidSettings());
    }

    @Test
    void familyNamesAreShownInSpanish() {
        assertEquals("espada", ToffyCommand.familyName("sword"));
        assertEquals("hacha", ToffyCommand.familyName("axe"));
        assertEquals("maza", ToffyCommand.familyName("mace"));
        assertEquals("lanza", ToffyCommand.familyName("spear"));
        assertEquals("tridente", ToffyCommand.familyName("trident"));
        assertEquals("pico", ToffyCommand.familyName("pickaxe"));
        assertEquals("cana de pescar", ToffyCommand.familyName("fishing_rod"));
        assertEquals("bow", ToffyCommand.familyName("bow"));
    }
}
