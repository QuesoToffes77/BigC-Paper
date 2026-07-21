package dev.linqfy.bigCasares.modules.resourcepack;

import java.net.URI;
import java.util.Objects;
import java.util.function.Function;

public final class PackDeliveryUriResolver {
    private PackDeliveryUriResolver() {
    }

    public static URI resolve(ActivePackManifest active, Function<String, URI> currentPublisher) {
        Objects.requireNonNull(active, "active");
        Objects.requireNonNull(currentPublisher, "currentPublisher");
        return currentPublisher.apply(active.javaFile());
    }
}
