package dev.linqfy.bigCasares.modules.resourcepack;

import java.net.URI;

public record EmbeddedPackHttpSettings(String bindAddress, int port, URI publicBaseUri, int workerThreads) {

    public EmbeddedPackHttpSettings {
        if (bindAddress == null || bindAddress.isBlank()) {
            throw new IllegalArgumentException("embedded HTTP bind-address cannot be blank");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("embedded HTTP port is invalid");
        }
        if (publicBaseUri != null && !publicBaseUri.isAbsolute()) {
            throw new IllegalArgumentException("embedded HTTP public-base-url must be absolute");
        }
        if (workerThreads < 1 || workerThreads > 64) {
            throw new IllegalArgumentException("embedded HTTP worker-threads must be between 1 and 64");
        }
    }

    public static EmbeddedPackHttpSettings defaults() {
        return new EmbeddedPackHttpSettings("127.0.0.1", 8123, null, 2);
    }
}
