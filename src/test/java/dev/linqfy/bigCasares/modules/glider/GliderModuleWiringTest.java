package dev.linqfy.bigCasares.modules.glider;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GliderModuleWiringTest {

    @Test
    void moduleUsesProjectLifecycleAndIsRegistered() throws Exception {
        assertEquals("glider", new GliderModule(null).getId());
        String main = Files.readString(Path.of(
            "src", "main", "java", "dev", "linqfy", "bigCasares", "BigCasares.java"));
        String config = Files.readString(Path.of("src", "main", "resources", "config.yml"));
        assertTrue(main.contains("new GliderModule(this)"));
        assertTrue(config.contains("glider:"));
    }
}
