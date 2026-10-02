package dev.linqfy.bigCasares.items.catalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record ItemRecipeDefinition(
    String key,
    int resultAmount,
    RecipeType type,
    List<String> shape,
    Map<Character, String> ingredients,
    List<String> shapelessIngredients,
    String resultMaterial
) {

    public ItemRecipeDefinition(String key, int resultAmount, List<String> shape, Map<Character, String> ingredients) {
        this(key, resultAmount, RecipeType.SHAPED, shape, ingredients, List.of(), null);
    }

    public static ItemRecipeDefinition shapeless(String key, int resultAmount, List<String> ingredients) {
        return new ItemRecipeDefinition(key, resultAmount, RecipeType.SHAPELESS, List.of(), Map.of(), ingredients, null);
    }

    public static ItemRecipeDefinition shapeless(String key, int resultAmount, List<String> ingredients, String resultMaterial) {
        return new ItemRecipeDefinition(key, resultAmount, RecipeType.SHAPELESS, List.of(), Map.of(), ingredients, resultMaterial);
    }

    public ItemRecipeDefinition {
        key = normalizeKey(key);
        if (resultAmount < 1 || resultAmount > 64) {
            throw new IllegalArgumentException("resultAmount must be between 1 and 64");
        }
        type = Objects.requireNonNull(type, "type");
        shape = List.copyOf(Objects.requireNonNull(shape, "shape"));
        shapelessIngredients = Objects.requireNonNull(shapelessIngredients, "shapelessIngredients").stream()
            .map(ItemRecipeDefinition::normalizeIngredient).toList();
        resultMaterial = normalizeResultMaterial(resultMaterial);
        Map<Character, String> normalizedIngredients = new LinkedHashMap<>();
        for (Map.Entry<Character, String> entry : Objects.requireNonNull(ingredients, "ingredients").entrySet()) {
            Character keyCharacter = Objects.requireNonNull(entry.getKey(), "ingredient key");
            String material = normalizeIngredient(entry.getValue());
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
        String normalized = Objects.requireNonNull(value, "key").trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("recipe key is invalid: " + value);
        }
        return normalized;
    }

    private static String normalizeResultMaterial(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9_]+")) {
            throw new IllegalArgumentException("recipe result material is invalid: " + value);
        }
        return normalized;
    }

    /**
     * Normalizes one recipe ingredient. Supported syntax:
     * <ul>
     *   <li>a vanilla material: {@code SUGAR}, {@code COAL};</li>
     *   <li>a choice of vanilla materials separated by {@code |}: {@code COAL|CHARCOAL};</li>
     *   <li>a BigCasares catalog item reference: {@code bigcasares:potassium_nitrate}.</li>
     * </ul>
     */
    private static String normalizeIngredient(String value) {
        String trimmed = Objects.requireNonNull(value, "ingredient").trim();
        if (trimmed.contains("|")) {
            List<String> parts = new ArrayList<>();
            for (String rawPart : trimmed.split("\\|")) {
                String part = rawPart.trim();
                if (part.isEmpty()) {
                    throw new IllegalArgumentException("recipe ingredient choice is empty: " + value);
                }
                parts.add(normalizeIngredient(part));
            }
            return String.join("|", parts);
        }
        if (trimmed.contains(":")) {
            String normalized = trimmed.toLowerCase(Locale.ROOT);
            if (!normalized.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                throw new IllegalArgumentException("custom recipe ingredient is invalid: " + value);
            }
            return normalized;
        }
        String normalized = trimmed.toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9_]+")) {
            throw new IllegalArgumentException("recipe ingredient material is invalid: " + value);
        }
        return normalized;
    }
}
