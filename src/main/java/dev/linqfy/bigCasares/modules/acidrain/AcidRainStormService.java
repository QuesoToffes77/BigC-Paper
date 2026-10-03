package dev.linqfy.bigCasares.modules.acidrain;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public final class AcidRainStormService {
    private final Clock clock;
    private final Random random;
    private AcidRainSettings settings;
    private AcidRainState state = AcidRainState.INACTIVE;
    private AcidRainLevel level = AcidRainLevel.ACID;
    private Instant stateStartedAt;
    private Instant activeStartedAt;
    private Instant warningEndsAt;
    private Instant activeEndsAt;
    private Instant endingEndsAt;
    private Instant nextAutomaticAllowedAt;
    private final Map<String, AcidRainContamination> contamination = new LinkedHashMap<>();

    public AcidRainStormService(AcidRainSettings settings, Clock clock) {
        this(settings, clock, new Random());
    }

    AcidRainStormService(AcidRainSettings settings, Clock clock, Random random) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.random = Objects.requireNonNull(random, "random");
    }

    public synchronized AcidRainStartResult beginWarning(AcidRainLevel requestedLevel, Instant now) {
        Objects.requireNonNull(now, "now");
        if (state != AcidRainState.INACTIVE) {
            return AcidRainStartResult.rejected("acid rain is already running");
        }
        this.level = requestedLevel == null ? settings.defaultLevel() : requestedLevel;
        this.state = AcidRainState.WARNING;
        this.stateStartedAt = now;
        this.warningEndsAt = now.plusSeconds(Math.max(0, settings.warning().durationSeconds()));
        this.activeStartedAt = null;
        this.activeEndsAt = null;
        this.endingEndsAt = null;
        if (settings.warning().durationSeconds() <= 0) {
            activateWarning(now);
        }
        return AcidRainStartResult.started(state, level);
    }

    public synchronized AcidRainStartResult startNow(AcidRainLevel requestedLevel, Instant now) {
        Objects.requireNonNull(now, "now");
        if (state != AcidRainState.INACTIVE) {
            return AcidRainStartResult.rejected("acid rain is already running");
        }
        this.level = requestedLevel == null ? settings.defaultLevel() : requestedLevel;
        activate(now);
        return AcidRainStartResult.started(state, level);
    }

    public synchronized AcidRainTransitionResult activateWarning(Instant now) {
        Objects.requireNonNull(now, "now");
        if (state != AcidRainState.WARNING) {
            return AcidRainTransitionResult.ignored("warning is not active");
        }
        activate(now);
        return AcidRainTransitionResult.transitioned(state, level);
    }

    public synchronized AcidRainTransitionResult stop(Instant now, String reason) {
        Objects.requireNonNull(now, "now");
        if (state == AcidRainState.INACTIVE) {
            return AcidRainTransitionResult.ignored("acid rain is not running");
        }
        if (state == AcidRainState.WARNING) {
            finish(now);
            return AcidRainTransitionResult.transitioned(state, level);
        }
        if (state == AcidRainState.ENDING) {
            finish(now);
            return AcidRainTransitionResult.transitioned(state, level);
        }
        this.state = AcidRainState.ENDING;
        this.stateStartedAt = now;
        this.endingEndsAt = now.plusSeconds(Math.max(0, settings.duration().endingSeconds()));
        contamination.clear();
        if (settings.duration().endingSeconds() <= 0) {
            finish(now);
        }
        return AcidRainTransitionResult.transitioned(state, level);
    }

    public synchronized void tick(Instant now) {
        Objects.requireNonNull(now, "now");
        expireContamination(now);
        if (state == AcidRainState.WARNING && !now.isBefore(warningEndsAt)) {
            activateWarning(now);
        }
        if (state == AcidRainState.ACTIVE) {
            escalateIfDue(now);
            if (!now.isBefore(activeEndsAt)) {
                state = AcidRainState.ENDING;
                stateStartedAt = now;
                endingEndsAt = now.plusSeconds(Math.max(0, settings.duration().endingSeconds()));
                contamination.clear();
            }
        }
        if (state == AcidRainState.ENDING && !now.isBefore(endingEndsAt)) {
            finish(now);
        }
    }

    public synchronized AcidRainStartResult attemptAutomaticStart(Instant now, int rollPercent) {
        AcidRainAutomaticSettings automatic = settings.automatic();
        if (!automatic.enabled() || state != AcidRainState.INACTIVE) {
            return AcidRainStartResult.rejected("automatic start unavailable");
        }
        if (nextAutomaticAllowedAt != null && now.isBefore(nextAutomaticAllowedAt)) {
            return AcidRainStartResult.rejected("automatic cooldown");
        }
        scheduleNextAutomaticWindow(now);
        if (rollPercent < 0 || rollPercent >= automatic.chancePercent()) {
            return AcidRainStartResult.rejected("automatic roll missed");
        }
        return beginWarning(settings.defaultLevel(), now);
    }

    public synchronized AcidRainSnapshot snapshot() {
        return snapshot(clock.instant());
    }

    public synchronized AcidRainSnapshot snapshot(Instant now) {
        Instant end = switch (state) {
            case WARNING -> warningEndsAt;
            case ACTIVE -> activeEndsAt;
            case ENDING -> endingEndsAt;
            case INACTIVE -> now;
        };
        int remaining = Math.max(0, (int) Duration.between(now, end).toSeconds());
        int total = switch (state) {
            case WARNING -> Math.max(0, settings.warning().durationSeconds());
            case ACTIVE -> Math.max(0, (int) Duration.between(activeStartedAt, activeEndsAt).toSeconds());
            case ENDING -> Math.max(0, settings.duration().endingSeconds());
            case INACTIVE -> 0;
        };
        return new AcidRainSnapshot(state, level, stateStartedAt, end, remaining, total, settings.worlds());
    }

    public synchronized void updateSettings(AcidRainSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public synchronized AcidRainSettings settings() {
        return settings;
    }

    public synchronized void contaminate(AcidRainContamination state) {
        if (state != null) {
            contamination.put(state.subjectId(), state);
        }
    }

    public synchronized int temporaryStateCount() {
        return contamination.size();
    }

    public synchronized void clearTemporaryState() {
        contamination.clear();
    }

    public synchronized void shutdown() {
        finish(clock.instant());
    }

    private void activate(Instant now) {
        this.state = AcidRainState.ACTIVE;
        this.stateStartedAt = now;
        this.activeStartedAt = now;
        this.warningEndsAt = null;
        this.endingEndsAt = null;
        int duration = randomDuration();
        this.activeEndsAt = now.plusSeconds(duration);
    }

    private int randomDuration() {
        int minimum = Math.max(0, settings.duration().minimumSeconds());
        int maximum = Math.max(minimum, settings.duration().maximumSeconds());
        if (maximum == minimum) {
            return minimum;
        }
        return minimum + random.nextInt(maximum - minimum + 1);
    }

    private void escalateIfDue(Instant now) {
        if (!settings.escalation().enabled() || activeStartedAt == null) {
            return;
        }
        int elapsed = Math.max(0, (int) Duration.between(activeStartedAt, now).toSeconds());
        for (AcidRainEscalationStep step : settings.escalation().steps()) {
            if (step.level() != null && elapsed >= step.afterSeconds() && step.level().ordinal() > level.ordinal()) {
                level = step.level();
            }
        }
    }

    private void expireContamination(Instant now) {
        contamination.values().removeIf(value -> !now.isBefore(value.expiresAt()));
    }

    private void finish(Instant now) {
        state = AcidRainState.INACTIVE;
        stateStartedAt = now;
        activeStartedAt = null;
        warningEndsAt = null;
        activeEndsAt = null;
        endingEndsAt = null;
        contamination.clear();
    }

    private void scheduleNextAutomaticWindow(Instant now) {
        AcidRainAutomaticSettings automatic = settings.automatic();
        int min = Math.max(1, automatic.minimumIntervalMinutes());
        int max = Math.max(min, automatic.maximumIntervalMinutes());
        int minutes = min == max ? min : min + random.nextInt(max - min + 1);
        nextAutomaticAllowedAt = now.plusSeconds(minutes * 60L);
    }
}

record AcidRainStartResult(boolean started, String message, AcidRainState state, AcidRainLevel level) {
    static AcidRainStartResult started(AcidRainState state, AcidRainLevel level) {
        return new AcidRainStartResult(true, "", state, level);
    }

    static AcidRainStartResult rejected(String message) {
        return new AcidRainStartResult(false, message, AcidRainState.INACTIVE, AcidRainLevel.ACID);
    }
}

record AcidRainTransitionResult(boolean transitioned, String message, AcidRainState state, AcidRainLevel level) {
    static AcidRainTransitionResult transitioned(AcidRainState state, AcidRainLevel level) {
        return new AcidRainTransitionResult(true, "", state, level);
    }

    static AcidRainTransitionResult ignored(String message) {
        return new AcidRainTransitionResult(false, message, AcidRainState.INACTIVE, AcidRainLevel.ACID);
    }
}

record AcidRainSnapshot(
    AcidRainState state,
    AcidRainLevel level,
    Instant stateStartedAt,
    Instant endsAt,
    int remainingSeconds,
    int totalSeconds,
    AcidRainWorldSettings worlds
) {
    boolean active() {
        return state == AcidRainState.ACTIVE;
    }
}

record AcidRainContamination(String subjectId, double intensity, Instant expiresAt) {
}
