package dev.linqfy.bigCasares.modules.resourcepack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourcePackValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsDuplicateLogicalIdentifiers() throws IOException {
        Path registry = tempDir.resolve("registry.yml");
        Files.writeString(registry, """
            assets:
              nexus:
                type: entity-model
                java-model: bigcasares:nexus
              nexus:
                type: boss-model
                java-model: bigcasares:abyss_guardian
            """);

        assertThrows(IllegalArgumentException.class, () -> ResourcePackRegistry.load(registry));
    }

    @Test
    void rejectsRegisteredAssetsWhoseRequiredFilesAreMissing() throws IOException {
        ResourcePackFixture fixture = ResourcePackFixture.create(tempDir, false);
        ResourcePackRegistry registry = ResourcePackRegistry.load(fixture.registry());

        assertThrows(IllegalArgumentException.class,
            () -> new ResourcePackValidator().validate(registry, fixture.javaRoot(), fixture.bedrockRoot()));
    }
}
