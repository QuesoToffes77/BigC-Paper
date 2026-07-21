package dev.linqfy.bigCasares.modules.resourcepack;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PackDeliveryUriResolverTest {

    @Test
    void derivesCurrentEmbeddedUrlInsteadOfTrustingPersistedManifestUrl() throws Exception {
        ActivePackManifest active = new ActivePackManifest(
            "0123456789abcdef", "a".repeat(64), "b".repeat(40), "c".repeat(64),
            UUID.randomUUID(), "bigcasares-java-0123456789abcdef.zip",
            "d".repeat(64), UUID.randomUUID(), "bigcasares-bedrock-0123456789abcdef.mcpack",
            null, Instant.parse("2026-07-18T12:00:00Z")
        );
        Class<?> resolver = Class.forName(
            "dev.linqfy.bigCasares.modules.resourcepack.PackDeliveryUriResolver"
        );
        Method resolve = resolver.getMethod("resolve", ActivePackManifest.class, Function.class);
        Function<String, URI> currentPublisher = file ->
            URI.create("https://packs.example.test/packs/" + file);

        assertEquals(
            URI.create("https://packs.example.test/packs/bigcasares-java-0123456789abcdef.zip"),
            resolve.invoke(null, active, currentPublisher)
        );
    }
}
