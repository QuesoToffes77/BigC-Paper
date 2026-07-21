package dev.linqfy.bigCasares.modules.resourcepack;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePackModuleWiringTest {

    @Test
    void exposesStableModuleId() {
        assertEquals("resource-pack-system", new ResourcePackModule(null).getId());
    }

    @Test
    void defaultConfigurationPublishesPacksToClients() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
            new File("src/main/resources/config.yml")
        );

        ResourcePackSettings settings = ResourcePackSettings.load(config);

        assertEquals(ResourcePackPublisher.Mode.COPY_ONLY, settings.publisher().mode());
    }

    @Test
    void declaresThePublicManualResourcePackCommand() {
        YamlConfiguration description = YamlConfiguration.loadConfiguration(
            new File("src/main/resources/plugin.yml")
        );

        assertTrue(description.isConfigurationSection("commands.resourcepack"));
        assertEquals("/<command>", description.getString("commands.resourcepack.usage"));
    }
}
