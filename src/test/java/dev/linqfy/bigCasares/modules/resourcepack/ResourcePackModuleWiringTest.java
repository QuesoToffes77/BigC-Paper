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
    void defaultConfigurationPublishesPacksToClientsAutomatically() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
            new File("src/main/resources/config.yml")
        );

        ResourcePackSettings settings = ResourcePackSettings.load(config);

        assertEquals(ResourcePackPublisher.Mode.EMBEDDED_HTTP, settings.publisher().mode());
        assertEquals("http://127.0.0.1:8123/", settings.embeddedHttp().publicBaseUri().toString());
    }

    @Test
    void requiredLegacyCopyOnlyConfigurationFallsBackToLocalAutomaticDelivery() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("resource-pack-system.required", true);
        config.set("resource-pack-system.publishing.mode", "copy-only");
        config.set("resource-pack-system.publishing.port", 9123);

        ResourcePackSettings settings = ResourcePackSettings.load(config);

        assertEquals(ResourcePackPublisher.Mode.EMBEDDED_HTTP, settings.publisher().mode());
        assertEquals("http://127.0.0.1:9123/", settings.embeddedHttp().publicBaseUri().toString());
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
