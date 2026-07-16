package dev.linqfy.bigCasares.items.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemCatalogSeederTest {

    @TempDir
    Path tempDir;

    @Test
    void seedsMissingDefaultsWithoutOverwritingAnOperatorEdit() throws Exception {
        Files.writeString(tempDir.resolve("copper_apple.yml"), "operator edit");
        ItemCatalogSeeder seeder = new ItemCatalogSeeder(List.of("copper_apple.yml", "smoke_bomb.yml"));

        ItemCatalogSeedResult result = seeder.seed(
            path -> new ByteArrayInputStream(("default " + path).getBytes(StandardCharsets.UTF_8)), tempDir);

        assertEquals("operator edit", Files.readString(tempDir.resolve("copper_apple.yml")));
        assertEquals("default smoke_bomb.yml", Files.readString(tempDir.resolve("smoke_bomb.yml")));
        assertEquals(1, result.copiedFiles());
        assertEquals(1, result.preservedFiles());
    }

    @Test
    void rejectsUnsafeDefaultNamesAndMissingResources() {
        assertThrows(IllegalArgumentException.class,
            () -> new ItemCatalogSeeder(List.of("../escape.yml")));
        ItemCatalogSeeder seeder = new ItemCatalogSeeder(List.of("copper_apple.yml"));

        assertThrows(IllegalStateException.class, () -> seeder.seed(ignored -> null, tempDir));
        assertFalse(Files.exists(tempDir.resolve("copper_apple.yml")));
    }
}
