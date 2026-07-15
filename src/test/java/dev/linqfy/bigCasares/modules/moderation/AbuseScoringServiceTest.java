package dev.linqfy.bigCasares.modules.moderation;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AbuseScoringServiceTest {

    @Test
    void emitsWarningAndCriticalAlertsWithoutPunishment() {
        AbuseScoringService service = new AbuseScoringService(
            Duration.ofSeconds(60), 50, 90, Duration.ofSeconds(120)
        );
        UUID player = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-15T12:00:00Z");

        assertFalse(service.record(signal(now, player, 25)).alert().isPresent());
        AbuseScoreResult warning = service.record(signal(now.plusSeconds(1), player, 25));
        AbuseScoreResult critical = service.record(signal(now.plusSeconds(2), player, 40));

        assertEquals(50, warning.score());
        assertEquals(AbuseSeverity.WARNING, warning.alert().orElseThrow());
        assertEquals(AbuseSeverity.CRITICAL, critical.alert().orElseThrow());
        assertFalse(critical.punishmentRequested());
    }

    @Test
    void expiresSignalsOutsideTheRollingWindow() {
        AbuseScoringService service = new AbuseScoringService(
            Duration.ofSeconds(60), 50, 90, Duration.ofSeconds(120)
        );
        UUID player = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-15T12:00:00Z");
        service.record(signal(now, player, 40));

        AbuseScoreResult result = service.record(signal(now.plusSeconds(61), player, 20));

        assertEquals(20, result.score());
    }

    @Test
    void cooldownSuppressesRepeatedAlertAtTheSameSeverity() {
        AbuseScoringService service = new AbuseScoringService(
            Duration.ofSeconds(60), 50, 90, Duration.ofSeconds(120)
        );
        UUID player = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-15T12:00:00Z");
        service.record(signal(now, player, 50));
        service.record(signal(now.plusSeconds(61), player, 1));

        AbuseScoreResult suppressed = service.record(signal(now.plusSeconds(62), player, 50));

        assertTrue(suppressed.alert().isEmpty());
        assertEquals(51, suppressed.score());
    }

    private static AbuseSignal signal(Instant at, UUID player, int points) {
        return new AbuseSignal(at, player, "Player", "test", points, "evidence");
    }
}
