package dev.linqfy.bigCasares.items.catalog;

import java.util.Objects;

public record ItemAppearanceDefinition(String javaItemDefinition, String bedrockTexture) {

    public ItemAppearanceDefinition {
        javaItemDefinition = normalizePath(javaItemDefinition, "javaItemDefinition");
        bedrockTexture = normalizePath(bedrockTexture, "bedrockTexture");
    }

    private static String normalizePath(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).replace('\\', '/').trim();
        if (normalized.isEmpty() || normalized.startsWith("/") || normalized.contains("//")
            || normalized.contains("../") || normalized.equals("..")) {
            throw new IllegalArgumentException(name + " must be a safe relative path");
        }
        return normalized;
    }
}
