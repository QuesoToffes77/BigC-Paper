package dev.linqfy.bigCasares.modules.moderation;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Observation(Instant timestamp, UUID playerId, String type, Map<String, String> values) {

    public Observation {
        timestamp = timestamp == null ? Instant.now() : timestamp;
        type = type == null ? "unknown" : type;
        values = values == null ? Map.of() : Map.copyOf(values);
    }
}
