package dev.linqfy.bigCasares.modules.moderation;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class AbuseScoringService {
    private final Duration window;
    private final int warningThreshold;
    private final int criticalThreshold;
    private final Duration notificationCooldown;
    private final Map<UUID, Deque<AbuseSignal>> signals = new HashMap<>();
    private final Map<UUID, EnumMap<AbuseSeverity, Instant>> lastAlerts = new HashMap<>();

    public AbuseScoringService(
        Duration window,
        int warningThreshold,
        int criticalThreshold,
        Duration notificationCooldown
    ) {
        if (window.isNegative() || window.isZero() || notificationCooldown.isNegative()) {
            throw new IllegalArgumentException("Scoring durations must be valid");
        }
        if (warningThreshold <= 0 || criticalThreshold <= warningThreshold) {
            throw new IllegalArgumentException("Scoring thresholds are invalid");
        }
        this.window = window;
        this.warningThreshold = warningThreshold;
        this.criticalThreshold = criticalThreshold;
        this.notificationCooldown = notificationCooldown;
    }

    public synchronized AbuseScoreResult record(AbuseSignal signal) {
        Deque<AbuseSignal> playerSignals = signals.computeIfAbsent(signal.playerId(), ignored -> new ArrayDeque<>());
        purge(playerSignals, signal.timestamp());
        int previousScore = playerSignals.stream().mapToInt(AbuseSignal::points).sum();
        playerSignals.addLast(signal);
        int score = previousScore + signal.points();
        Optional<AbuseSeverity> candidate = alertFor(previousScore, score);
        Optional<AbuseSeverity> alert = candidate.filter(severity -> cooldownElapsed(signal.playerId(), severity, signal.timestamp()));
        alert.ifPresent(severity -> lastAlerts
            .computeIfAbsent(signal.playerId(), ignored -> new EnumMap<>(AbuseSeverity.class))
            .put(severity, signal.timestamp()));
        return new AbuseScoreResult(score, alert, false);
    }

    public synchronized int score(UUID playerId, Instant now) {
        Deque<AbuseSignal> playerSignals = signals.get(playerId);
        if (playerSignals == null) {
            return 0;
        }
        purge(playerSignals, now);
        return playerSignals.stream().mapToInt(AbuseSignal::points).sum();
    }

    private Optional<AbuseSeverity> alertFor(int previousScore, int score) {
        if (previousScore < criticalThreshold && score >= criticalThreshold) {
            return Optional.of(AbuseSeverity.CRITICAL);
        }
        if (previousScore < warningThreshold && score >= warningThreshold) {
            return Optional.of(AbuseSeverity.WARNING);
        }
        return Optional.empty();
    }

    private boolean cooldownElapsed(UUID playerId, AbuseSeverity severity, Instant now) {
        Instant last = lastAlerts.getOrDefault(playerId, new EnumMap<>(AbuseSeverity.class)).get(severity);
        return last == null || !now.isBefore(last.plus(notificationCooldown));
    }

    private void purge(Deque<AbuseSignal> playerSignals, Instant now) {
        Instant cutoff = now.minus(window);
        while (!playerSignals.isEmpty() && playerSignals.peekFirst().timestamp().isBefore(cutoff)) {
            playerSignals.removeFirst();
        }
    }
}
