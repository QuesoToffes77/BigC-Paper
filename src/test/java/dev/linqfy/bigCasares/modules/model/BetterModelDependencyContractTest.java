package dev.linqfy.bigCasares.modules.model;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BetterModelDependencyContractTest {

    @Test
    void pluginDescriptorRequiresBetterModel() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(input);
            var yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(input, StandardCharsets.UTF_8));

            assertEquals(List.of("BetterModel"), yaml.getStringList("depend"));
        }
    }
}
