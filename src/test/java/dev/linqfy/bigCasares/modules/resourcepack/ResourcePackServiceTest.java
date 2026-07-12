package dev.linqfy.bigCasares.modules.resourcepack;

import dev.linqfy.bigCasares.platform.ClientPlatform;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePackServiceTest {

    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000071");
    private static final UUID BEDROCK_PACK_ID = UUID.fromString("7e1cc35b-7a9f-4af4-b930-93f936f9cb64");

    @Test
    void javaPackIdIsStableForTheManifestVersionAndChangesWithIt() {
        ResourcePackManifest first = manifest("075a3f4dec4b");
        ResourcePackManifest sameVersion = manifest("075a3f4dec4b");
        ResourcePackManifest nextVersion = manifest("175a3f4dec4b");

        assertEquals(first.javaUuid(), sameVersion.javaUuid());
        assertEquals(UUID.fromString("ee96c3af-3ab9-363d-9ca3-903cf0bbe165"), first.javaUuid());
        assertFalse(first.javaUuid().equals(nextVersion.javaUuid()));
        assertFalse(first.javaUuid().equals(first.bedrockUuid()));
    }

    @Test
    void requestsTheJavaPackWithTheManifestDerivedId() {
        ResourcePackManifest manifest = manifest("075a3f4dec4b");
        AtomicReference<UUID> requestedPackId = new AtomicReference<>();
        ResourcePackService service = service(manifest,
            (playerId, packId, uri, sha1, prompt, required) -> requestedPackId.set(packId));

        assertTrue(service.requestFor(PLAYER_ID));
        assertEquals(manifest.javaUuid(), requestedPackId.get());
        assertTrue(service.isBigCasaresPack(manifest.javaUuid()));
        assertFalse(service.isBigCasaresPack(UUID.fromString("00000000-0000-0000-0000-000000000099")));
    }

    @Test
    void ignoresStatusEventsForResourcePacksOwnedByOtherPlugins() {
        ResourcePackManifest manifest = manifest("075a3f4dec4b");
        ResourcePackService service = service(manifest, (playerId, packId, uri, sha1, prompt, required) -> { });
        ResourcePackStatusHandler handler = new ResourcePackStatusHandler(service);

        handler.handle(
            UUID.fromString("00000000-0000-0000-0000-000000000099"),
            PLAYER_ID,
            "SUCCESSFULLY_LOADED",
            () -> { }
        );

        assertEquals(ResourcePackPlayerState.NOT_REQUESTED, service.state(PLAYER_ID));
    }

    @Test
    void acceptsStatusEventsForTheBigCasaresPack() {
        ResourcePackManifest manifest = manifest("075a3f4dec4b");
        ResourcePackService service = service(manifest, (playerId, packId, uri, sha1, prompt, required) -> { });
        ResourcePackStatusHandler handler = new ResourcePackStatusHandler(service);

        handler.handle(
            manifest.javaUuid(),
            PLAYER_ID,
            "SUCCESSFULLY_LOADED",
            () -> { }
        );

        assertEquals(ResourcePackPlayerState.LOADED, service.state(PLAYER_ID));
    }

    @Test
    void listenerRejectsForeignPackIdsBeforeReadingPlayerStatus() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/dev/linqfy/bigCasares/modules/resourcepack/ResourcePackStatusListener.java"
        ));

        int idGuard = source.indexOf("if (!service.isBigCasaresPack(event.getID()))");
        int playerAccess = source.indexOf("event.getPlayer()", idGuard);

        assertTrue(idGuard >= 0, "listener must guard status events by pack ID");
        assertTrue(playerAccess > idGuard, "foreign pack events must return before player status handling");
    }

    private static ResourcePackService service(ResourcePackManifest manifest, ResourcePackGateway gateway) {
        return new ResourcePackService(
            new ResourcePackSettings(
                false,
                true,
                "BigCasares pack",
                new ResourcePackPublisher(ResourcePackPublisher.Mode.EXTERNAL_URL,
                    URI.create("https://example.com/bigcasares.zip"))
            ),
            manifest,
            gateway,
            ignored -> ClientPlatform.JAVA
        );
    }

    private static ResourcePackManifest manifest(String version) {
        return new ResourcePackManifest(
            version,
            "0".repeat(64),
            "1".repeat(40),
            BEDROCK_PACK_ID
        );
    }

}
