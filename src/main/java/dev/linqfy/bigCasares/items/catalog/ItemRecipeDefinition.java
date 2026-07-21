package dev.linqfy.bigCasares.items.catalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ItemRecipeDefinition(
    String key,
    int resultAmount,
    RecipeType type,
    List<String> shape,
    Map<Character, String> ingredients,
    List<String> shapelessIngredients
) {

    public ItemRecipeDefinition(String key, int resultAmount, List<String> shape, Map<Character, String> ingredients) {
        this(key, resultAmount, RecipeType.SHAPED, shape, ingredients, List.of());
    }

    public static ItemRecipeDefinition shapeless(String key, int resultAmount, List<String> ingredients) {
        return new ItemRecipeDefinition(key, resultAmount, RecipeType.SHAPELESS, List.of(), Map.of(), ingredients);
    }

    public ItemRecipeDefinition {
        key = normalizeKey(key);
        if (resultAmount < 1 || resultAmount > 64) {
            throw new IllegalArgumentException("resultAmount must be between 1 and 64");
        }
        type = Objects.requireNonNull(type, "type");
        shape = List.copyOf(Objects.requireNonNull(shape, "shape"));
        shapelessIngredients = Objects.requireNonNull(shapelessIngredients, "shapelessIngredients").stream()
            .map(ItemRecipeDefinition::normalizeMaterial).toList();
        Map<Character, String> normalizedIngredients = new LinkedHashMap<>();
        for (Map.Entry<Character, String> entry : Objects.requireNonNull(ingredients, "ingredients").entrySet()) {
            Character keyCharacter = Objects.requireNonNull(entry.getKey(), "ingredient key");
            String material = normalizeMaterial(entry.getValue());
            if (keyCharacter == ' ') {
                throw new IllegalArgumentException("recipe ingredients must use non-space keys");
            }
            if (normalizedIngredients.put(keyCharacter, material) != null) {
                throw new IllegalArgumentException("duplicate recipe ingredient: " + keyCharacter);
            }
        }
        if (type == RecipeType.SHAPED) {
            if (shape.isEmpty() || shape.size() > 3 || shape.stream().anyMatch(row -> row == null || row.length() > 3)) {
                throw new IllegalArgumentException("shape must contain one to three rows with at most three columns");
            }
            if (!shapelessIngredients.isEmpty()) {
                throw new IllegalArgumentException("shaped recipes cannot define shapeless ingredients");
            }
            for (String row : shape) {
                for (int index = 0; index < row.length(); index++) {
                    char character = row.charAt(index);
                    if (character != ' ' && !normalizedIngredients.containsKey(character)) {
                        throw new IllegalArgumentException("recipe shape references an undefined ingredient: " + character);
                    }
                }
            }
        } else {
            if (!shape.isEmpty() || !normalizedIngredients.isEmpty()) {
                throw new IllegalArgumentException("shapeless recipes cannot define a shape or keyed ingredients");
            }
            if (shapelessIngredients.isEmpty() || shapelessIngredients.size() > 9) {
                throw new IllegalArgumentException("shapeless recipes must contain one to nine ingredients");
            }
        }
        ingredients = Map.copyOf(normalizedIngredients);
    }

    private static String normalizeKey(String value) {
        String normalized = Objects.requireNonNull(value, "key").trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalized.matches("[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("recipe key is invalid: " + value);
        }
        return normalized;
    }

    private static String normalizeMaterial(String value) {
        String normalized = Objects.requireNonNull(value, "ingredient material").trim().toUpperCase(java.util.Locale.ROOT);
        if (!normalized.matches("[A-Z0-9_]+")) {
            throw new IllegalArgumentException("recipe ingredient material is invalid: " + value);
        }
        return normalized;
    }
}
