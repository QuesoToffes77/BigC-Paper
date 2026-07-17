package dev.linqfy.bigCasares.items.catalog;

import dev.linqfy.bigCasares.modules.resourcepack.JsonSyntaxValidator;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ItemCatalogValidator {

    private static final Map<String, String> EXPECTED_MECHANICS = Map.of(
        "copper_apple", "copper-apple",
        "smoke_bomb", "smoke-bomb",
        "prismarine_arrow", "prismarine-arrow",
        "nexus", "nexus"
    );

    private final ItemMaterialValidator materials;

    public ItemCatalogValidator() {
        this(material -> material.matches("[A-Z0-9_]+"));
    }

    public ItemCatalogValidator(ItemMaterialValidator materials) {
        this.materials = java.util.Objects.requireNonNull(materials, "materials");
    }

    public void validate(CustomItemCatalog catalog, Path contentPackRoot) {
        Path packRoot = contentPackRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(packRoot)) {
            throw new IllegalArgumentException("content pack directory does not exist: " + packRoot);
        }
        Set<String> models = new HashSet<>();
        Set<Integer> legacyModelData = new HashSet<>();
        Set<String> recipeKeys = new HashSet<>();
        for (CustomItemDefinition definition : catalog.definitions().values()) {
            String expectedMechanic = EXPECTED_MECHANICS.get(definition.id());
            if (expectedMechanic == null || !expectedMechanic.equals(definition.mechanic())) {
                throw new IllegalArgumentException("unsupported item mechanic for " + definition.id());
            }
            if (!materials.isItem(definition.material())) {
                throw new IllegalArgumentException("item material is not available: " + definition.material());
            }
            if (!models.add(definition.itemModel().toString())) {
                throw new IllegalArgumentException("duplicate item-model: " + definition.itemModel());
            }
            if (definition.legacyCustomModelData().isPresent()
                && !legacyModelData.add(definition.legacyCustomModelData().getAsInt())) {
                throw new IllegalArgumentException("duplicate legacy custom-model-data: "
                    + definition.legacyCustomModelData().getAsInt());
            }
            definition.recipeDefinition().ifPresent(recipe -> {
                if (!recipeKeys.add(recipe.key())) {
                    throw new IllegalArgumentException("duplicate item recipe key: " + recipe.key());
                }
                for (String material : recipe.ingredients().values()) {
                    if (!materials.isItem(material)) {
                        throw new IllegalArgumentException("recipe material is not available: " + material);
                    }
                }
            });
            validateAppearance(packRoot, definition.appearance());
        }
    }

    private static void validateAppearance(Path packRoot, ItemAppearanceDefinition appearance) {
        Path javaDefinition = validateFile(packRoot, appearance.javaItemDefinition(), ".json");
        Path bedrockTexture = validateFile(packRoot, appearance.bedrockTexture(), ".png");
        try {
            new JsonSyntaxValidator().validate(Files.readString(javaDefinition), appearance.javaItemDefinition());
            if (ImageIO.read(new ByteArrayInputStream(Files.readAllBytes(bedrockTexture))) == null) {
                throw new IllegalArgumentException("unreadable item appearance texture: " + appearance.bedrockTexture());
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("could not validate item appearance assets", exception);
        }
    }

    private static Path validateFile(Path root, String relativePath, String extension) {
        if (!relativePath.endsWith(extension)) {
            throw new IllegalArgumentException("appearance asset has wrong extension: " + relativePath);
        }
        Path candidate = root.resolve(relativePath).normalize();
        if (!candidate.startsWith(root) || !Files.isRegularFile(candidate)) {
            throw new IllegalArgumentException("missing item appearance asset: " + relativePath);
        }
        return candidate;
    }
}
