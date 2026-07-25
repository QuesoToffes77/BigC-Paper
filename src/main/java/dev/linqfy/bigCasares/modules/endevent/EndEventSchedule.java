package dev.linqfy.bigCasares.modules.endevent;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;

public final class EndEventSchedule {
    private final LocalDate date;
    private final ZoneId zone;

    public EndEventSchedule(LocalDate date, ZoneId zone) {
        this.date = Objects.requireNonNull(date, "date");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    public Instant countdownStartsAt() {
        return date.atTime(LocalTime.of(21, 30)).atZone(zone).toInstant();
    }

    public Instant revealAt() {
        return date.atTime(LocalTime.of(22, 0)).atZone(zone).toInstant();
    }

    public Instant delayedRevealAt() {
        return date.atTime(LocalTime.of(22, 15)).atZone(zone).toInstant();
    }

    public String formatRemaining(Instant now, boolean delayed) {
        Instant target = delayed ? delayedRevealAt() : revealAt();
        long seconds = Math.max(0L, Duration.between(now, target).getSeconds());
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60L, seconds % 60L);
    }
}
