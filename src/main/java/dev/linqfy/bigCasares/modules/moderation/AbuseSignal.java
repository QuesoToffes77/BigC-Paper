package dev.linqfy.bigCasares.modules.moderation;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AbuseSignal(
    Instant timestamp,
    UUID playerId,
    String playerName,
    String rule,
    int points,
    String evidence
) {

    public AbuseSignal {
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(playerId, "playerId");
        playerName = playerName == null ? "" : playerName;
        rule = rule == null ? "unknown" : rule;
        evidence = evidence == null ? "" : evidence;
        if (points <= 0) {
            throw new IllegalArgumentException("Abuse signal points must be positive");
        }
    }
}
