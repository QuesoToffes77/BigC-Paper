package dev.linqfy.bigCasares.modules.resourcepack;

import java.util.Objects;

public record ResourcePackAsset(
    String id,
    String type,
    String javaModel,
    String bedrockEntity,
    String texture,
    String javaSound,
    String bedrockSound
) {
    private static final String BUILTIN_TEXTURE_PREFIX = "minecraft:";

    public ResourcePackAsset {
        id = requireText(id, "id");
        type = requireText(type, "type");
        javaModel = normalize(javaModel);
        bedrockEntity = normalize(bedrockEntity);
        texture = normalize(texture);
        validateBuiltinTexture(texture);
        javaSound = normalize(javaSound);
        bedrockSound = normalize(bedrockSound);
    }

    public boolean usesBuiltinTexture() {
        return texture != null && texture.startsWith(BUILTIN_TEXTURE_PREFIX);
    }

    public String texturePath() {
        return usesBuiltinTexture() ? texture.substring(BUILTIN_TEXTURE_PREFIX.length()) : texture;
    }

    private static void validateBuiltinTexture(String texture) {
        if (texture == null || !texture.startsWith(BUILTIN_TEXTURE_PREFIX)) {
            return;
        }
        String path = texture.substring(BUILTIN_TEXTURE_PREFIX.length());
        if (path.isBlank() || path.startsWith("/") || path.startsWith("\\")
            || path.contains("\\") || path.contains("..") || path.contains("//")) {
            throw new IllegalArgumentException("Invalid built-in texture path: " + texture);
        }
    }

    private static String requireText(String value, String name) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return normalized;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Objects.requireNonNull(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
