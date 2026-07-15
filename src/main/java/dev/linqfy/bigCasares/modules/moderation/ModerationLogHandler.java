package dev.linqfy.bigCasares.modules.moderation;

import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Collection;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public final class ModerationLogHandler extends Handler {
    private final Supplier<Collection<? extends Player>> onlinePlayers;
    private final Consumer<AbuseSignal> signals;
    private final AuditSink audit;

    public ModerationLogHandler(
        Supplier<Collection<? extends Player>> onlinePlayers,
        Consumer<AbuseSignal> signals,
        AuditSink audit
    ) {
        this.onlinePlayers = onlinePlayers;
        this.signals = signals;
        this.audit = audit;
        setLevel(Level.WARNING);
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record) || record.getMessage() == null) {
            return;
        }
        String message = record.getMessage();
        if (message.startsWith("[DiscordQueue]")) {
            return;
        }
        AuditSeverity severity = record.getLevel().intValue() >= Level.SEVERE.intValue()
            ? AuditSeverity.SEVERE : AuditSeverity.WARNING;
        audit.publish(AuditEvent.system(severity, "server-log", "server-warning", message));
        String normalized = message.toLowerCase(Locale.ROOT);
        if (!(normalized.contains("illegal packet") || normalized.contains("authentication")
            || normalized.contains("spoof") || normalized.contains("invalid movement"))) {
            return;
        }
        for (Player player : onlinePlayers.get()) {
            if (normalized.contains(player.getName().toLowerCase(Locale.ROOT))) {
                signals.accept(new AbuseSignal(
                    Instant.now(), player.getUniqueId(), player.getName(), "security-log", 40, message
                ));
                break;
            }
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
