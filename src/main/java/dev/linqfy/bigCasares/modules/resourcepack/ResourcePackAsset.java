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
    public ResourcePackAsset {
        id = requireText(id, "id");
        type = requireText(type, "type");
        javaModel = normalize(javaModel);
        bedrockEntity = normalize(bedrockEntity);
        texture = normalize(texture);
        javaSound = normalize(javaSound);
        bedrockSound = normalize(bedrockSound);
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
