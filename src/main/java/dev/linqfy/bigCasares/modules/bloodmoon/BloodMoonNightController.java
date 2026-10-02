package dev.linqfy.bigCasares.modules.bloodmoon;

import java.util.Objects;

final class BloodMoonNightController {
    private final BloodMoonSchedulingSettings settings;
    private long lastEvaluatedNight = Long.MIN_VALUE;
    private int normalNightsSinceEvent;
    private int intervalNights;
    private boolean active;

    BloodMoonNightController(BloodMoonSchedulingSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.normalNightsSinceEvent = settings.minimumNormalNightsBetweenEvents();
    }

    BloodMoonTransition observe(long fullTime, double roll, boolean conflict) {
        if (!isNight(fullTime)) {
            if (active) {
                active = false;
                return BloodMoonTransition.STOPPED;
            }
            return BloodMoonTransition.NONE;
        }
        if (active) {
            return BloodMoonTransition.NONE;
        }

        long nightIndex = Math.floorDiv(fullTime, 24_000L);
        if (lastEvaluatedNight == nightIndex) {
            return BloodMoonTransition.NONE;
        }
        lastEvaluatedNight = nightIndex;

        boolean starts = false;
        if (settings.mode() == BloodMoonSchedulingMode.INTERVAL) {
            intervalNights++;
            starts = intervalNights >= settings.everyNights();
        } else if (normalNightsSinceEvent >= settings.minimumNormalNightsBetweenEvents()) {
            starts = Double.isFinite(roll) && roll >= 0.0 && roll < settings.chancePerNight();
        }
        if (conflict) {
            starts = false;
        }
        if (starts) {
            active = true;
            normalNightsSinceEvent = 0;
            intervalNights = 0;
            return BloodMoonTransition.STARTED;
        }
        normalNightsSinceEvent++;
        return BloodMoonTransition.NONE;
    }

    boolean forceStart(long fullTime) {
        if (active || !isNight(fullTime)) {
            return false;
        }
        active = true;
        lastEvaluatedNight = Math.floorDiv(fullTime, 24_000L);
        normalNightsSinceEvent = 0;
        intervalNights = 0;
        return true;
    }

    boolean forceStop() {
        if (!active) {
            return false;
        }
        active = false;
        return true;
    }

    boolean active() {
        return active;
    }

    static boolean isNight(long fullTime) {
        long timeOfDay = Math.floorMod(fullTime, 24_000L);
        return timeOfDay >= 13_000L && timeOfDay < 23_000L;
    }
}

enum BloodMoonTransition {
    NONE,
    STARTED,
    STOPPED
}
