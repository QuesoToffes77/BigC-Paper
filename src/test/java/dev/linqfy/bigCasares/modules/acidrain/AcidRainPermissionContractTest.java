package dev.linqfy.bigCasares.modules.acidrain;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AcidRainPermissionContractTest {

    @Test
    void bypassPermissionIsExplicitAndNotInheritedByAdminOrWildcard() {
        YamlConfiguration pluginYaml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));
        ConfigurationSection permissions = pluginYaml.getConfigurationSection("permissions");

        assertNotNull(permissions);
        assertEquals("false", String.valueOf(permissions.get("bigcasares.acidrain.bypass.default")));
        assertFalse(permissions.getBoolean("bigcasares.*.children.bigcasares.acidrain.bypass", false));
        assertFalse(permissions.getBoolean("bigcasares.acidrain.admin.children.bigcasares.acidrain.bypass", false));
    }
}
