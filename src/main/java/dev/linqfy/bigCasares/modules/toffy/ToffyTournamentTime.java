package dev.linqfy.bigCasares.modules.toffy;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Times of the scheduled tournament teleports: parsing what the admin typed and formatting stored epoch millis.
 * Pure: the current time and the zone are parameters.
 */
final class ToffyTournamentTime {
    static final String HOUR_USAGE = "La hora tiene que ser HH:mm (por ejemplo 21:30).";
    static final String DAY_USAGE = "La fecha tiene que ser dd/MM (por ejemplo 24/12).";
    private static final Pattern HOUR = Pattern.compile("(\\d{1,2}):(\\d{2})");
    private static final Pattern DAY = Pattern.compile("(\\d{1,2})/(\\d{1,2})");
    /** Leap years come every four years, so a 29/02 is always found within this many years. */
    private static final int YEARS_TO_SEARCH = 8;

    private ToffyTournamentTime() {
    }

    /**
     * Epoch millis of the next moment that matches. Without a day: today at {@code hour}, or tomorrow when that
     * moment already passed. With a day: that day this year, or the next year when it already passed.
     *
     * @param hour HH:mm
     * @param day  dd/MM, or null for "today or tomorrow"
     * @throws IllegalArgumentException when the hour or the day is not valid (31/02 and 25:00 are refused)
     */
    static long resolve(String hour, String day, long now, ZoneId zone) {
        LocalTime time = time(hour);
        LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        if (day == null) {
            long todayAt = at(today, time, zone);
            return todayAt > now ? todayAt : at(today.plusDays(1), time, zone);
        }
        Matcher matcher = DAY.matcher(day);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(DAY_USAGE);
        }
        int dayOfMonth = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        if (!exists(2000, month, dayOfMonth)) {
            throw new IllegalArgumentException("La fecha " + day + " no existe.");
        }
        for (int year = today.getYear(); year <= today.getYear() + YEARS_TO_SEARCH; year++) {
            if (!exists(year, month, dayOfMonth)) {
                continue;
            }
            long at = at(LocalDate.of(year, month, dayOfMonth), time, zone);
            if (at > now) {
                return at;
            }
        }
        throw new IllegalArgumentException("La fecha " + day + " no existe.");
    }

    static String hour(long at, ZoneId zone) {
        ZonedDateTime time = Instant.ofEpochMilli(at).atZone(zone);
        return String.format(Locale.ROOT, "%02d:%02d", time.getHour(), time.getMinute());
    }

    /** dd/MM/yyyy */
    static String date(long at, ZoneId zone) {
        ZonedDateTime time = Instant.ofEpochMilli(at).atZone(zone);
        return String.format(Locale.ROOT, "%02d/%02d/%04d", time.getDayOfMonth(), time.getMonthValue(), time.getYear());
    }

    /** dd/MM/yyyy HH:mm */
    static String dateTime(long at, ZoneId zone) {
        return date(at, zone) + " " + hour(at, zone);
    }

    private static LocalTime time(String hour) {
        Matcher matcher = hour == null ? null : HOUR.matcher(hour);
        if (matcher == null || !matcher.matches()) {
            throw new IllegalArgumentException(HOUR_USAGE);
        }
        int hours = Integer.parseInt(matcher.group(1));
        int minutes = Integer.parseInt(matcher.group(2));
        if (hours > 23 || minutes > 59) {
            throw new IllegalArgumentException(HOUR_USAGE);
        }
        return LocalTime.of(hours, minutes);
    }

    private static boolean exists(int year, int month, int day) {
        try {
            LocalDate.of(year, month, day);
            return true;
        } catch (DateTimeException invalid) {
            return false;
        }
    }

    private static long at(LocalDate date, LocalTime time, ZoneId zone) {
        return ZonedDateTime.of(date, time, zone).toInstant().toEpochMilli();
    }
}
