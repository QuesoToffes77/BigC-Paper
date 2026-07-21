package dev.linqfy.bigCasares.modules.resourcepack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddedPackHttpServerTest {

    @TempDir
    Path tempDir;

    @Test
    void servesOnlyImmutableContentAddressedArtifactsAndKeepsOldArtifactReadable() throws Exception {
        Path artifacts = tempDir.resolve("artifacts");
        Files.createDirectories(artifacts);
        String oldFile = "bigcasares-java-1111111111111111.zip";
        String newFile = "bigcasares-java-2222222222222222.zip";
        Files.write(artifacts.resolve(oldFile), "old".getBytes(StandardCharsets.UTF_8));
        EmbeddedPackHttpServer server = new EmbeddedPackHttpServer(
            artifacts,
            new EmbeddedPackHttpSettings("127.0.0.1", 0, URI.create("https://packs.example.test"), 2)
        );
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<byte[]> oldBefore = get(client, server.localUri(oldFile));
            Files.write(artifacts.resolve(newFile), "new".getBytes(StandardCharsets.UTF_8));
            HttpResponse<byte[]> oldAfter = get(client, server.localUri(oldFile));
            HttpResponse<byte[]> current = get(client, server.localUri(newFile));
            HttpResponse<byte[]> missing = get(client,
                URI.create("http://127.0.0.1:" + server.boundPort() + "/packs/../active-pack.json"));

            assertEquals(200, oldBefore.statusCode());
            assertEquals(200, oldAfter.statusCode());
            assertEquals(200, current.statusCode());
            assertEquals(404, missing.statusCode());
            assertArrayEquals("old".getBytes(StandardCharsets.UTF_8), oldAfter.body());
            assertEquals("public, max-age=31536000, immutable",
                current.headers().firstValue("Cache-Control").orElseThrow());
            assertEquals("https://packs.example.test/packs/" + newFile, server.publicUri(newFile).toString());
        } finally {
            int port = server.boundPort();
            server.close();
            try (ServerSocket rebound = new ServerSocket()) {
                rebound.bind(new InetSocketAddress("127.0.0.1", port));
                assertFalse(rebound.isClosed());
            }
        }
    }

    @Test
    void servesBetterModelArtifactsPublishedByTheResourcePackModule() throws Exception {
        Path artifacts = tempDir.resolve("artifacts");
        Files.createDirectories(artifacts);
        String file = "bettermodel-java-866b1c84240fd8aa.zip";
        Files.write(artifacts.resolve(file), "bettermodel".getBytes(StandardCharsets.UTF_8));
        EmbeddedPackHttpServer server = new EmbeddedPackHttpServer(
            artifacts,
            new EmbeddedPackHttpSettings("127.0.0.1", 0, URI.create("https://packs.example.test"), 1)
        );
        server.start();
        try {
            HttpResponse<byte[]> response = get(HttpClient.newHttpClient(), server.localUri(file));

            assertEquals(200, response.statusCode());
            assertArrayEquals("bettermodel".getBytes(StandardCharsets.UTF_8), response.body());
            assertEquals("https://packs.example.test/packs/" + file, server.publicUri(file).toString());
        } finally {
            server.close();
        }
    }

    @Test
    void downloadWorkersCanBurstBeyondTheConfiguredIdlePool() throws Exception {
        ExecutorService workers = EmbeddedPackHttpServer.createWorkerExecutor(2);
        CountDownLatch entered = new CountDownLatch(3);
        CountDownLatch release = new CountDownLatch(1);
        try {
            for (int index = 0; index < 3; index++) {
                workers.submit(() -> {
                    entered.countDown();
                    try {
                        release.await();
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            assertTrue(entered.await(Duration.ofSeconds(2).toMillis(), TimeUnit.MILLISECONDS));
        } finally {
            release.countDown();
            workers.shutdownNow();
        }
    }

    private static HttpResponse<byte[]> get(HttpClient client, URI uri) throws Exception {
        return client.send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
    }
}
