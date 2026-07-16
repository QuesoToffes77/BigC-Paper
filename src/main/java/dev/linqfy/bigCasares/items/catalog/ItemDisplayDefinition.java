package dev.linqfy.bigCasares.items.catalog;

import java.util.List;
import java.util.Objects;

public record ItemDisplayDefinition(String translationKey, String fallbackName, List<String> lore) {

    public ItemDisplayDefinition {
        translationKey = requireText(translationKey, "translationKey");
        fallbackName = requireText(fallbackName, "fallbackName");
        lore = List.copyOf(Objects.requireNonNull(lore, "lore"));
        if (lore.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("lore must not contain null values");
        }
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
