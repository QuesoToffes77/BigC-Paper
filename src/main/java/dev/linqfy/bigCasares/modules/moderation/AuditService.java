package dev.linqfy.bigCasares.modules.moderation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AuditService implements AuditSink {
    private final List<AuditSink> sinks = new CopyOnWriteArrayList<>();

    public void addSink(AuditSink sink) {
        if (sink != null && sink != this) {
            sinks.add(sink);
        }
    }

    public void removeSink(AuditSink sink) {
        sinks.remove(sink);
    }

    @Override
    public void publish(AuditEvent event) {
        AuditEvent redacted = redact(event);
        for (AuditSink sink : sinks) {
            try {
                sink.publish(redacted);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private AuditEvent redact(AuditEvent event) {
        Map<String, String> details = event.details().entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
            Map.Entry::getKey,
            entry -> AuditRedactor.redact(entry.getValue())
        ));
        return new AuditEvent(
            event.timestamp(), event.severity(), event.category(), event.action(), event.playerId(),
            event.playerName(), AuditRedactor.redact(event.message()), details
        );
    }
}
