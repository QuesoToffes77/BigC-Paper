package dev.linqfy.bigCasares.modules.resourcepack;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ResourcePackManifest(
    String version,
    String inputSha256,
    String javaSha1,
    UUID bedrockUuid
) {
    private static final Pattern FIELD = Pattern.compile("\\\"%s\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    public ResourcePackManifest {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version cannot be blank");
        }
        if (inputSha256 == null || !inputSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("inputSha256 must be a lowercase SHA-256");
        }
        if (javaSha1 == null || !javaSha1.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException("javaSha1 must be a lowercase SHA-1");
        }
        if (bedrockUuid == null) {
            throw new IllegalArgumentException("bedrockUuid cannot be null");
        }
    }

    public String toJson() {
        return """
            {
              "version": "%s",
              "inputSha256": "%s",
              "java": {
                "file": "bigcasares-java.zip",
                "sha1": "%s"
              },
              "bedrock": {
                "file": "bigcasares-bedrock.mcpack",
                "uuid": "%s"
              }
            }
            """.formatted(version, inputSha256, javaSha1, bedrockUuid);
    }

    /**
     * Stable identity for the Java pack represented by this manifest version.
     * Minecraft uses this ID to correlate status events with a specific pack.
     */
    public UUID javaUuid() {
        return UUID.nameUUIDFromBytes(
            ("bigcasares:resource-pack:java:" + version).getBytes(StandardCharsets.UTF_8)
        );
    }

    public static ResourcePackManifest fromJson(String json) {
        return new ResourcePackManifest(
            value(json, "version"),
            value(json, "inputSha256"),
            value(json, "sha1"),
            UUID.fromString(value(json, "uuid"))
        );
    }

    private static String value(String json, String name) {
        Matcher matcher = Pattern.compile(FIELD.pattern().formatted(Pattern.quote(name))).matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing manifest field: " + name);
        }
        return matcher.group(1);
    }
}
