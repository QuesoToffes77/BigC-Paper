package dev.linqfy.bigCasares.modules.resourcepack;

import java.net.URI;
import java.util.UUID;

@FunctionalInterface
public interface ResourcePackGateway {
    void requestJavaPack(
        UUID playerId,
        UUID packId,
        URI publicUri,
        byte[] sha1,
        String prompt,
        boolean required
    );

    default void removeJavaPack(UUID playerId, UUID packId) {
    }
}
