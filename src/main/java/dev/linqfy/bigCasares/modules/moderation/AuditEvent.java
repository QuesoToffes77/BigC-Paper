package dev.linqfy.bigCasares.modules.moderation;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record AuditEvent(
    Instant timestamp,
    AuditSeverity severity,
    String category,
    String action,
    Optional<UUID> playerId,
    String playerName,
    String message,
    Map<String, String> details
) {

    public AuditEvent {
        timestamp = timestamp == null ? Instant.now() : timestamp;
        severity = severity == null ? AuditSeverity.INFO : severity;
        category = category == null ? "general" : category;
        action = action == null ? "event" : action;
        playerId = playerId == null ? Optional.empty() : playerId;
        playerName = playerName == null ? "" : playerName;
        message = message == null ? "" : message;
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    public static AuditEvent system(AuditSeverity severity, String category, String action, String message) {
        return new AuditEvent(Instant.now(), severity, category, action, Optional.empty(), "", message, Map.of());
    }

    public static AuditEvent player(
        AuditSeverity severity,
        String category,
        String action,
        UUID playerId,
        String playerName,
        String message
    ) {
        return new AuditEvent(
            Instant.now(), severity, category, action, Optional.ofNullable(playerId), playerName, message, Map.of()
        );
    }
}
