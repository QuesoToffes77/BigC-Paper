package dev.linqfy.bigCasares.items.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultItemCatalogTest {

    @TempDir
    Path tempDir;

    @Test
    void packagedDefaultsSeedAndValidateEveryCatalogItem() {
        List<String> names = List.of(
            "copper_apple.yml",
            "smoke_bomb.yml",
            "prismarine_arrow.yml",
            "nexus.yml",
            "echo_arrow.yml",
            "golden_tipped_amethyst_arrow.yml",
            "tracker_compass.yml",
            "nuke_shot.yml",
            "sahurs_bat.yml"
        );
        ItemCatalogSeeder seeder = new ItemCatalogSeeder(names);
        seeder.seed(name -> resource("content/items/" + name), tempDir.resolve("items"));

        CustomItemCatalog catalog = new ItemCatalogLoader().load(tempDir.resolve("items"));
        new ItemCatalogValidator().validate(catalog, Path.of("resourcepack"));

        assertEquals(9, catalog.size());
        assertTrue(catalog.find("copper_apple").isPresent());
        assertTrue(catalog.find("smoke_bomb").isPresent());
        assertTrue(catalog.find("prismarine_arrow").isPresent());
        assertTrue(catalog.find("nexus").isPresent());
        assertTrue(catalog.find("echo_arrow").isPresent());
        assertTrue(catalog.find("golden_tipped_amethyst_arrow").isPresent());
        assertTrue(catalog.find("tracker_compass").isPresent());
        assertTrue(catalog.find("nuke_shot").isPresent());
        assertTrue(catalog.find("sahurs_bat").isPresent());
        CustomItemDefinition nukeShot = catalog.require("nuke_shot");
        assertEquals(1, nukeShot.maxStackSize());
        assertEquals("FISHING_ROD", nukeShot.material());
        assertEquals("minecraft:fishing_rod", nukeShot.itemModel());
        assertEquals(null, nukeShot.maxDamage());
        assertEquals(List.of("§cUn disparo. Trescientos cincuenta problemas."),
            nukeShot.display().lore());
        CustomItemDefinition bat = catalog.require("sahurs_bat");
        assertEquals(6.0, bat.combatDefinition().orElseThrow().attackDamage());
        assertEquals(5.0, bat.combatDefinition().orElseThrow().attackSpeed());
        assertTrue(bat.recipeDefinition().isEmpty());
    }

    @Test
    void sahursBatItemTexturesExactlyMatchTheUploadedSakurBytes() throws Exception {
        byte[] expected = Files.readAllBytes(Path.of("agent_uploads/boss1/sakur.png"));

        assertArrayEquals(expected, Files.readAllBytes(
            Path.of("resourcepack/java/assets/bigcasares/textures/item/sahurs_bat.png")));
        assertArrayEquals(expected, Files.readAllBytes(
            Path.of("resourcepack/bedrock/textures/item/sahurs_bat.png")));
    }

    private static InputStream resource(String path) {
        InputStream input = DefaultItemCatalogTest.class.getClassLoader().getResourceAsStream(path);
        if (input == null) {
            throw new IllegalStateException("missing test resource: " + path);
        }
        return input;
    }
}
