package dev.linqfy.bigCasares.modules.resourcepack;

import dev.linqfy.bigCasares.platform.ClientPlatform;
import dev.linqfy.bigCasares.platform.ClientPlatformGateway;

import java.net.URI;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ResourcePackService {
    private final ResourcePackSettings settings;
    private final ResourcePackManifest manifest;
    private final ResourcePackGateway gateway;
    private final ClientPlatformGateway platformGateway;
    private final Map<UUID, ResourcePackPlayerState> states = new ConcurrentHashMap<>();

    public ResourcePackService(
        ResourcePackSettings settings,
        ResourcePackManifest manifest,
        ResourcePackGateway gateway,
        ClientPlatformGateway platformGateway
    ) {
        this.settings = settings;
        this.manifest = manifest;
        this.gateway = gateway;
        this.platformGateway = platformGateway;
    }

    public boolean requestFor(UUID playerId) {
        if (platformGateway.resolvePlatform(playerId) == ClientPlatform.BEDROCK) {
            return false;
        }
        Optional<URI> uri = settings.publisher().javaPackUri();
        if (uri.isEmpty()) {
            return false;
        }
        ResourcePackPlayerState current = state(playerId);
        if (!settings.resendOnVersionChange() && current != ResourcePackPlayerState.NOT_REQUESTED) {
            return false;
        }
        gateway.requestJavaPack(playerId, manifest.javaUuid(), uri.get(), HexFormat.of().parseHex(manifest.javaSha1()),
            settings.promptMessage(), settings.required());
        states.put(playerId, ResourcePackPlayerState.SENT);
        return true;
    }

    public void updateState(UUID playerId, ResourcePackPlayerState state) {
        states.put(playerId, state);
    }

    public ResourcePackPlayerState state(UUID playerId) {
        return states.getOrDefault(playerId, ResourcePackPlayerState.NOT_REQUESTED);
    }

    public boolean hasLoadedPack(UUID playerId) {
        return state(playerId) == ResourcePackPlayerState.LOADED;
    }

    public boolean isRequired() {
        return settings.required();
    }

    public boolean isBigCasaresPack(UUID packId) {
        return manifest.javaUuid().equals(packId);
    }

    public void forget(UUID playerId) {
        states.remove(playerId);
    }
}
