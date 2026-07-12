package dev.linqfy.bigCasares.modules.nexus;

import java.util.Objects;
import java.util.UUID;

public record NexusId(UUID value) {

    public NexusId {
        Objects.requireNonNull(value, "value");
    }

    public static NexusId random() {
        return new NexusId(UUID.randomUUID());
    }

    public static NexusId parse(String value) {
        return new NexusId(UUID.fromString(Objects.requireNonNull(value, "value")));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
