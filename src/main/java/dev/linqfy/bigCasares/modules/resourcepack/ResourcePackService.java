package dev.linqfy.bigCasares.modules.resourcepack;

import dev.linqfy.bigCasares.platform.ClientPlatform;
import dev.linqfy.bigCasares.platform.ClientPlatformGateway;

import java.net.URI;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class ResourcePackService {
    private final ResourcePackSettings settings;
    private final ResourcePackGateway gateway;
    private final ClientPlatformGateway platformGateway;
    private final Map<UUID, ResourcePackPlayerState> states = new ConcurrentHashMap<>();
    private final AtomicReference<Delivery> delivery;
    private volatile UUID previousPackId;

    public ResourcePackService(
        ResourcePackSettings settings,
        ResourcePackManifest manifest,
        ResourcePackGateway gateway,
        ClientPlatformGateway platformGateway
    ) {
        this.settings = settings;
        this.gateway = gateway;
        this.platformGateway = platformGateway;
        this.delivery = new AtomicReference<>(new Delivery(
            manifest,
            settings.publisher().javaPackUri().orElse(null)
        ));
    }

    public boolean requestFor(UUID playerId) {
        return requestFor(playerId, false);
    }

    public boolean forceRequestFor(UUID playerId) {
        return requestFor(playerId, true);
    }

    private boolean requestFor(UUID playerId, boolean force) {
        if (platformGateway.resolvePlatform(playerId) == ClientPlatform.BEDROCK) {
            return false;
        }
        Delivery currentDelivery = delivery.get();
        if (currentDelivery.uri() == null) {
            return false;
        }
        ResourcePackPlayerState current = state(playerId);
        if (!force && !settings.resendOnVersionChange()
            && current != ResourcePackPlayerState.NOT_REQUESTED) {
            return false;
        }
        ResourcePackManifest manifest = currentDelivery.manifest();
        gateway.requestJavaPack(playerId, manifest.javaUuid(), currentDelivery.uri(),
            HexFormat.of().parseHex(manifest.javaSha1()),
            settings.promptMessage(), settings.required());
        states.put(playerId, ResourcePackPlayerState.SENT);
        return true;
    }

    public void updateState(UUID playerId, ResourcePackPlayerState state) {
        states.put(playerId, state);
    }

    public boolean updateState(UUID packId, UUID playerId, ResourcePackPlayerState state) {
        if (!delivery.get().manifest().javaUuid().equals(packId)) {
            return false;
        }
        states.put(playerId, state);
        return true;
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
        return delivery.get().manifest().javaUuid().equals(packId) || packId.equals(previousPackId);
    }

    public void activate(ResourcePackManifest manifest, URI uri) {
        Delivery previous = delivery.getAndSet(new Delivery(manifest, uri));
        previousPackId = previous.manifest().javaUuid();
    }

    public ResourcePackManifest currentManifest() {
        return delivery.get().manifest();
    }

    public Optional<URI> currentUri() {
        return Optional.ofNullable(delivery.get().uri());
    }

    public void forget(UUID playerId) {
        states.remove(playerId);
    }

    private record Delivery(ResourcePackManifest manifest, URI uri) {
    }
}
