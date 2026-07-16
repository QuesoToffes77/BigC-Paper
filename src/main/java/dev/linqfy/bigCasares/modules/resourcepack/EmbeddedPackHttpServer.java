package dev.linqfy.bigCasares.modules.resourcepack;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class EmbeddedPackHttpServer implements AutoCloseable {

    private final Path artifactsRoot;
    private final EmbeddedPackHttpSettings settings;
    private HttpServer server;
    private ExecutorService workers;

    public EmbeddedPackHttpServer(Path artifactsRoot, EmbeddedPackHttpSettings settings) {
        this.artifactsRoot = Objects.requireNonNull(artifactsRoot, "artifactsRoot")
            .toAbsolutePath().normalize();
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public synchronized void start() throws IOException {
        if (server != null) {
            return;
        }
        Files.createDirectories(artifactsRoot);
        InetAddress address = InetAddress.getByName(settings.bindAddress());
        HttpServer created = HttpServer.create(new InetSocketAddress(address, settings.port()), 0);
        AtomicInteger sequence = new AtomicInteger();
        ExecutorService createdWorkers = Executors.newFixedThreadPool(settings.workerThreads(), runnable -> {
            Thread thread = new Thread(runnable, "bigcasares-pack-http-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        created.createContext("/packs/", this::handle);
        created.setExecutor(createdWorkers);
        created.start();
        server = created;
        workers = createdWorkers;
    }

    public synchronized int boundPort() {
        requireStarted();
        return server.getAddress().getPort();
    }

    public URI publicUri(String artifactFile) {
        URI base = settings.publicBaseUri();
        if (base == null) {
            throw new IllegalStateException("embedded HTTP public-base-url is required");
        }
        return resolve(base, artifactFile);
    }

    URI localUri(String artifactFile) {
        return resolve(URI.create("http://127.0.0.1:" + boundPort() + "/"), artifactFile);
    }

    @Override
    public synchronized void close() {
        HttpServer closing = server;
        ExecutorService closingWorkers = workers;
        server = null;
        workers = null;
        if (closing != null) {
            closing.stop(0);
        }
        if (closingWorkers != null) {
            closingWorkers.shutdownNow();
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String method = exchange.getRequestMethod();
            if (!"GET".equals(method) && !"HEAD".equals(method)) {
                exchange.getResponseHeaders().set("Allow", "GET, HEAD");
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String rawPath = exchange.getRequestURI().getRawPath();
            String prefix = "/packs/";
            String fileName = rawPath.startsWith(prefix) ? rawPath.substring(prefix.length()) : "";
            if (!safeArtifactName(fileName)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            Path artifact = artifactsRoot.resolve(fileName).normalize();
            if (!artifact.startsWith(artifactsRoot) || !Files.isRegularFile(artifact)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            long size = Files.size(artifact);
            exchange.getResponseHeaders().set("Content-Type",
                fileName.endsWith(".zip") ? "application/zip" : "application/octet-stream");
            exchange.getResponseHeaders().set("Cache-Control", "public, max-age=31536000, immutable");
            exchange.getResponseHeaders().set("ETag", "\"" + fileName + "\"");
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            if ("HEAD".equals(method)) {
                exchange.getResponseHeaders().set("Content-Length", Long.toString(size));
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            exchange.sendResponseHeaders(200, size);
            try (var input = Files.newInputStream(artifact); var output = exchange.getResponseBody()) {
                input.transferTo(output);
            }
        }
    }

    private static boolean safeArtifactName(String value) {
        return value.matches("bigcasares-(?:java|bedrock)-[0-9a-f]{16}\\.(?:zip|mcpack)");
    }

    private static URI resolve(URI base, String artifactFile) {
        if (!safeArtifactName(artifactFile)) {
            throw new IllegalArgumentException("Unsafe artifact filename: " + artifactFile);
        }
        String text = base.toString();
        if (!text.endsWith("/")) {
            text += "/";
        }
        return URI.create(text).resolve("packs/" + artifactFile);
    }

    private void requireStarted() {
        if (server == null) {
            throw new IllegalStateException("embedded pack HTTP server is not started");
        }
    }
}
