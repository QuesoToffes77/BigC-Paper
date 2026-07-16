package dev.linqfy.bigCasares.items.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemCatalogValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void acceptsDefinitionsWhoseAppearanceAssetsExist() throws Exception {
        Path pack = appearancePack();

        assertDoesNotThrow(() -> new ItemCatalogValidator().validate(
            new CustomItemCatalog(Map.of("copper_apple", definition("copper_apple", 1001))), pack));
    }

    @Test
    void rejectsDuplicateItemModelsAndLegacyModelData() throws Exception {
        Path pack = appearancePack();
        CustomItemDefinition first = definition("copper_apple", 1001);
        CustomItemDefinition second = new CustomItemDefinition(
            "smoke_bomb", "smoke-bomb", "SNOWBALL",
            "bigcasares:copper_apple", OptionalInt.of(1001),
            display(), 16, null, null, appearance());

        assertThrows(IllegalArgumentException.class, () -> new ItemCatalogValidator().validate(
            new CustomItemCatalog(Map.of(first.id(), first, second.id(), second)), pack));
    }

    @Test
    void rejectsAnUnknownMechanicAndMissingAppearanceAsset() throws Exception {
        Path pack = appearancePack();
        CustomItemDefinition invalidMechanic = new CustomItemDefinition(
            "copper_apple", "arbitrary-yaml-action", "APPLE",
            "bigcasares:copper_apple", OptionalInt.of(1001),
            display(), 64, null, null, appearance());

        assertThrows(IllegalArgumentException.class, () -> new ItemCatalogValidator().validate(
            new CustomItemCatalog(Map.of(invalidMechanic.id(), invalidMechanic)), pack));

        Files.delete(pack.resolve("bedrock/textures/item/copper_apple.png"));
        assertThrows(IllegalArgumentException.class, () -> new ItemCatalogValidator().validate(
            new CustomItemCatalog(Map.of("copper_apple", definition("copper_apple", 1001))), pack));
    }

    @Test
    void rejectsMalformedJavaDefinitionAndUnreadableBedrockTexture() throws Exception {
        Path pack = appearancePack();
        Files.writeString(pack.resolve("java/assets/bigcasares/items/copper_apple.json"), "{");

        assertThrows(IllegalArgumentException.class, () -> new ItemCatalogValidator().validate(
            new CustomItemCatalog(Map.of("copper_apple", definition("copper_apple", 1001))), pack));

        Files.writeString(pack.resolve("java/assets/bigcasares/items/copper_apple.json"), "{}");
        Files.write(pack.resolve("bedrock/textures/item/copper_apple.png"), new byte[] {1});

        assertThrows(IllegalArgumentException.class, () -> new ItemCatalogValidator().validate(
            new CustomItemCatalog(Map.of("copper_apple", definition("copper_apple", 1001))), pack));
    }

    @Test
    void rejectsMaterialsUnavailableFromTheRuntimeAdapter() throws Exception {
        Path pack = appearancePack();

        assertThrows(IllegalArgumentException.class, () -> new ItemCatalogValidator(material -> false).validate(
            new CustomItemCatalog(Map.of("copper_apple", definition("copper_apple", 1001))), pack));
    }

    private Path appearancePack() throws Exception {
        Path pack = tempDir.resolve("pack");
        Files.createDirectories(pack.resolve("java/assets/bigcasares/items"));
        Files.createDirectories(pack.resolve("bedrock/textures/item"));
        Files.writeString(pack.resolve("java/assets/bigcasares/items/copper_apple.json"), "{}");
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png",
            pack.resolve("bedrock/textures/item/copper_apple.png").toFile());
        return pack;
    }

    private static CustomItemDefinition definition(String id, int modelData) {
        return new CustomItemDefinition(
            id, "copper-apple", "APPLE",
            "bigcasares:copper_apple", OptionalInt.of(modelData),
            display(), 64, null, null, appearance());
    }

    private static ItemDisplayDefinition display() {
        return new ItemDisplayDefinition("item.bigcasares.copper_apple", "Manzana de Cobre", List.of());
    }

    private static ItemAppearanceDefinition appearance() {
        return new ItemAppearanceDefinition(
            "java/assets/bigcasares/items/copper_apple.json",
            "bedrock/textures/item/copper_apple.png"
        );
    }
}
