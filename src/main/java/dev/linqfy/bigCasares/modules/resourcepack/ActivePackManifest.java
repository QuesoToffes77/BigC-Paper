package dev.linqfy.bigCasares.modules.resourcepack;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ActivePackManifest(
    String version,
    String inputSha256,
    String javaSha1,
    String javaSha256,
    UUID javaUuid,
    String javaFile,
    String bedrockSha256,
    UUID bedrockUuid,
    String bedrockFile,
    URI javaUri,
    Instant activatedAt
) {

    public ActivePackManifest {
        require(version, "version");
        requireDigest(inputSha256, 64, "inputSha256");
        requireDigest(javaSha1, 40, "javaSha1");
        requireDigest(javaSha256, 64, "javaSha256");
        requireDigest(bedrockSha256, 64, "bedrockSha256");
        javaUuid = Objects.requireNonNull(javaUuid, "javaUuid");
        bedrockUuid = Objects.requireNonNull(bedrockUuid, "bedrockUuid");
        javaFile = safeFile(javaFile, ".zip");
        bedrockFile = safeFile(bedrockFile, ".mcpack");
        if (javaUri != null && !javaUri.isAbsolute()) {
            throw new IllegalArgumentException("javaUri must be absolute");
        }
        activatedAt = Objects.requireNonNull(activatedAt, "activatedAt");
    }

    public ResourcePackManifest deliveryManifest() {
        return new ResourcePackManifest(version, inputSha256, javaSha1, bedrockUuid);
    }

    public String toJson() {
        return """
            {
              "version": "%s",
              "inputSha256": "%s",
              "javaSha1": "%s",
              "javaSha256": "%s",
              "javaUuid": "%s",
              "javaFile": "%s",
              "bedrockSha256": "%s",
              "bedrockUuid": "%s",
              "bedrockFile": "%s",
              "javaUri": "%s",
              "activatedAt": "%s"
            }
            """.formatted(
                version, inputSha256, javaSha1, javaSha256, javaUuid, javaFile,
                bedrockSha256, bedrockUuid, bedrockFile, javaUri == null ? "" : javaUri, activatedAt);
    }

    public static ActivePackManifest fromJson(String json) {
        new JsonSyntaxValidator().validate(json, "active-pack.json");
        String uri = value(json, "javaUri");
        return new ActivePackManifest(
            value(json, "version"),
            value(json, "inputSha256"),
            value(json, "javaSha1"),
            value(json, "javaSha256"),
            UUID.fromString(value(json, "javaUuid")),
            value(json, "javaFile"),
            value(json, "bedrockSha256"),
            UUID.fromString(value(json, "bedrockUuid")),
            value(json, "bedrockFile"),
            uri.isBlank() ? null : URI.create(uri),
            Instant.parse(value(json, "activatedAt"))
        );
    }

    private static String value(String json, String name) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(name)
            + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing active manifest field: " + name);
        }
        return matcher.group(1);
    }

    private static String safeFile(String value, String suffix) {
        require(value, "artifact file");
        if (!value.matches("[a-z0-9._-]+") || !value.endsWith(suffix)) {
            throw new IllegalArgumentException("Unsafe artifact filename: " + value);
        }
        return value;
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
    }

    private static void requireDigest(String value, int length, String name) {
        if (value == null || !value.matches("[0-9a-f]{" + length + "}")) {
            throw new IllegalArgumentException(name + " must be a lowercase digest");
        }
    }
}
