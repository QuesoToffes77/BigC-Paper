package dev.linqfy.bigCasares.modules.environment;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;

class EnvironmentalResourcePackSafetyTest {

    @Test
    void eventVisualsDoNotReplaceGlobalVanillaRainFogOrMoon() throws Exception {
        try (var paths = Files.walk(Path.of("resourcepack"))) {
            var normalized = paths.filter(Files::isRegularFile)
                .map(path -> path.toString().replace('\\', '/').toLowerCase(Locale.ROOT))
                .toList();

            assertFalse(normalized.stream().anyMatch(path -> path.endsWith("/textures/environment/rain.png")));
            assertFalse(normalized.stream().anyMatch(path -> path.endsWith("/textures/environment/snow.png")));
            assertFalse(normalized.stream().anyMatch(path -> path.contains("moon_phases")));
            assertFalse(normalized.stream().anyMatch(path -> path.contains("/shaders/")));
        }
    }
}
