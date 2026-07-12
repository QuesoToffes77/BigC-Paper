package dev.linqfy.bigCasares.modules.geyser;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record GeyserCustomEntityDefinition(
    String javaEntityType,
    String bedrockIdentifier,
    String markerKey
) {
    public GeyserCustomEntityDefinition {
        javaEntityType = requireText(javaEntityType, "javaEntityType").toUpperCase(Locale.ROOT);
        bedrockIdentifier = requireText(bedrockIdentifier, "bedrockIdentifier");
        markerKey = requireText(markerKey, "markerKey");
    }

    public boolean matches(String candidateJavaType, Set<String> presentMarkerKeys) {
        return javaEntityType.equalsIgnoreCase(Objects.requireNonNull(candidateJavaType, "candidateJavaType"))
            && Objects.requireNonNull(presentMarkerKeys, "presentMarkerKeys").contains(markerKey);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }
}
