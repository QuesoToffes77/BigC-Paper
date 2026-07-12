package dev.linqfy.bigCasares.modules.geyser;

public record GeyserCustomItemDefinition(
    String javaBaseIdentifier,
    String javaModelIdentifier,
    String bedrockIdentifier,
    String icon,
    String displayName
) {
    public GeyserCustomItemDefinition {
        javaBaseIdentifier = requireText(javaBaseIdentifier, "javaBaseIdentifier");
        javaModelIdentifier = requireText(javaModelIdentifier, "javaModelIdentifier");
        bedrockIdentifier = requireText(bedrockIdentifier, "bedrockIdentifier");
        icon = requireText(icon, "icon");
        displayName = requireText(displayName, "displayName");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }
}
