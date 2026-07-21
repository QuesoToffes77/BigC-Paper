package dev.linqfy.bigCasares.modules.resourcepack;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public record JavaPackDelivery(UUID id, URI uri, byte[] sha1, String owner) {
    public JavaPackDelivery {
        id = Objects.requireNonNull(id, "id");
        uri = Objects.requireNonNull(uri, "uri");
        sha1 = Arrays.copyOf(Objects.requireNonNull(sha1, "sha1"), sha1.length);
        if (sha1.length != 20) throw new IllegalArgumentException("sha1 must contain 20 bytes");
        if (owner == null || owner.isBlank()) throw new IllegalArgumentException("owner cannot be blank");
    }

    @Override public byte[] sha1() { return Arrays.copyOf(sha1, sha1.length); }
}
