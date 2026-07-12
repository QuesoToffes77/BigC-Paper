package dev.linqfy.bigCasares.modules.resourcepack;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;

public record ResourcePackPublisher(Mode mode, URI publicUri) {
    public enum Mode {
        EXTERNAL_URL,
        EMBEDDED_HTTP,
        COPY_ONLY
    }

    public ResourcePackPublisher {
        if (mode == Mode.EXTERNAL_URL && publicUri == null) {
            throw new IllegalArgumentException("external-url publishing requires public-url");
        }
        if (publicUri != null && !publicUri.isAbsolute()) {
            throw new IllegalArgumentException("resource-pack public-url must be absolute");
        }
    }

    public static ResourcePackPublisher create(String mode, String publicUrl) {
        Mode parsed = switch (mode.toLowerCase(Locale.ROOT).replace('-', '_')) {
            case "external_url" -> Mode.EXTERNAL_URL;
            case "embedded_http" -> Mode.EMBEDDED_HTTP;
            case "copy_only" -> Mode.COPY_ONLY;
            default -> throw new IllegalArgumentException("Unknown resource-pack publishing mode: " + mode);
        };
        return new ResourcePackPublisher(parsed,
            publicUrl == null || publicUrl.isBlank() ? null : URI.create(publicUrl.trim()));
    }

    public static ResourcePackPublisher disabled() {
        return new ResourcePackPublisher(Mode.COPY_ONLY, null);
    }

    public Optional<URI> javaPackUri() {
        return mode == Mode.EXTERNAL_URL ? Optional.of(publicUri) : Optional.empty();
    }
}
