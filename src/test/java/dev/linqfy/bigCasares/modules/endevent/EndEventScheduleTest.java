package dev.linqfy.bigCasares.modules.endevent;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EndEventScheduleTest {

    private final EndEventSchedule schedule = new EndEventSchedule(
        LocalDate.of(2026, 7, 25),
        ZoneId.of("America/Argentina/Buenos_Aires")
    );

    @Test
    void usesBuenosAiresWallClockForEveryScheduledInstant() {
        assertEquals(Instant.parse("2026-07-26T00:30:00Z"), schedule.countdownStartsAt());
        assertEquals(Instant.parse("2026-07-26T01:00:00Z"), schedule.revealAt());
        assertEquals(Instant.parse("2026-07-26T01:15:00Z"), schedule.delayedRevealAt());
    }

    @Test
    void formatsCountdownAsMinutesAndSecondsWithoutTickDrift() {
        assertEquals("29:59", schedule.formatRemaining(
            Instant.parse("2026-07-26T00:30:01Z"), false
        ));
        assertEquals("15:00", schedule.formatRemaining(
            Instant.parse("2026-07-26T01:00:00Z"), true
        ));
        assertEquals("00:00", schedule.formatRemaining(
            Instant.parse("2026-07-26T01:16:00Z"), true
        ));
    }
}
