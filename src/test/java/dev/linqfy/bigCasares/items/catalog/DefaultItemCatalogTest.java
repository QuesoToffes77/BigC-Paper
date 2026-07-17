package dev.linqfy.bigCasares.items.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultItemCatalogTest {

    @TempDir
    Path tempDir;

    @Test
    void packagedDefaultsSeedAndValidateTheFourMigratedItems() {
        List<String> names = List.of("copper_apple.yml", "smoke_bomb.yml", "prismarine_arrow.yml", "nexus.yml");
        ItemCatalogSeeder seeder = new ItemCatalogSeeder(names);
        seeder.seed(name -> resource("content/items/" + name), tempDir.resolve("items"));

        CustomItemCatalog catalog = new ItemCatalogLoader().load(tempDir.resolve("items"));
        new ItemCatalogValidator().validate(catalog, Path.of("resourcepack"));

        assertEquals(4, catalog.size());
        assertTrue(catalog.find("copper_apple").isPresent());
        assertTrue(catalog.find("smoke_bomb").isPresent());
        assertTrue(catalog.find("prismarine_arrow").isPresent());
        assertTrue(catalog.find("nexus").isPresent());
    }

    private static InputStream resource(String path) {
        InputStream input = DefaultItemCatalogTest.class.getClassLoader().getResourceAsStream(path);
        if (input == null) {
            throw new IllegalStateException("missing test resource: " + path);
        }
        return input;
    }
}
