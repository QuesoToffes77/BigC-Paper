package dev.linqfy.bigCasares.modules.celular;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import dev.linqfy.bigCasares.testsupport.CompiledClass;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CelularModuleWiringTest {

    @Test
    void exposesStableModuleId() {
        assertEquals("celular", new CelularModule(null).getId());
        assertEquals(CelularModule.MODULE_ID, new CelularModule(null).getId());
    }

    @Test
    void enableAndDisableWithoutAPluginAreNoOps() {
        CelularModule module = new CelularModule(null);
        assertDoesNotThrow(() -> module.onEnable(new RuntimeRegistrationScope()));
        assertDoesNotThrow(module::onDisable);
        assertEquals(0, module.videoCount());
    }

    @Test
    void moduleIsRegisteredDeclaredAndEnabledByDefault() throws Exception {
        CompiledClass main = CompiledClass.of(getClass(), "dev.linqfy.bigCasares.BigCasares");
        assertTrue(main.calls("dev/linqfy/bigCasares/modules/celular/CelularModule", "<init>"),
            "BigCasares has to create the module");
        assertTrue(main.calls("dev/linqfy/bigCasares/module/ModuleManager", "register"));
        assertEquals(CelularModule.class, BigCasares.class.getMethod("getCelularModule").getReturnType());

        // permission names contain dots, so plugin.yml is read with '/' as the path separator
        YamlConfiguration plugin = new YamlConfiguration();
        plugin.options().pathSeparator('/');
        plugin.load(new File("src/main/resources/plugin.yml"));
        assertEquals("bigcasares.celular.admin", plugin.getString("commands/celular/permission"));
        assertEquals("celu", plugin.getStringList("commands/celular/aliases").get(0));
        assertEquals("op", plugin.getString("permissions/bigcasares.celular.admin/default"));
        assertTrue(plugin.getBoolean("permissions/bigcasares.*/children/bigcasares.celular.admin"),
            "bigcasares.* has to include the celular admin permission");

        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
        assertTrue(config.getBoolean("modules.celular.enabled"));
    }
}
