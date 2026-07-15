package dev.linqfy.bigCasares.modules.servercontrol;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ServerControlService {
    private final ControlStorage storage;
    private final Clock clock;
    private ControlState state;
    private Optional<PvpTransition> recoveredExpiration;

    public ServerControlService(ControlStorage storage, Clock clock) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.state = Objects.requireNonNullElse(storage.load(), ControlState.defaults());
        this.recoveredExpiration = expirePvpOverride(clock.instant());
    }

    public synchronized ControlState state() {
        return state;
    }

    public synchronized boolean effectivePvp(Instant instant) {
        return state.pvpOverride()
            .filter(override -> override.isLiveAt(instant))
            .map(TimedPvpOverride::targetState)
            .orElse(state.pvpBaseline());
    }

    public synchronized PvpTransition setTimedPvp(boolean enabled, Duration duration, String actor) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Timed PvP duration must be positive");
        }
        Instant now = clock.instant();
        boolean previous = effectivePvp(now);
        TimedPvpOverride override = new TimedPvpOverride(enabled, now.plus(duration), actor);
        state = new ControlState(
            state.pvpBaseline(), Optional.of(override), state.endAccessEnabled(),
            state.elytraRocketsEnabled(), state.resistanceLevel(), state.staffAlerts()
        );
        storage.save(state);
        return new PvpTransition(previous, enabled, state.pvpBaseline(), Optional.of(duration), actor, false);
    }

    public synchronized PvpTransition setPermanentPvp(boolean enabled, String actor) {
        Instant now = clock.instant();
        boolean previous = effectivePvp(now);
        state = new ControlState(
            enabled, Optional.empty(), state.endAccessEnabled(), state.elytraRocketsEnabled(),
            state.resistanceLevel(), state.staffAlerts()
        );
        storage.save(state);
        return new PvpTransition(previous, enabled, enabled, Optional.empty(), actor, false);
    }

    public synchronized Optional<PvpTransition> expirePvpOverride(Instant now) {
        Optional<TimedPvpOverride> current = state.pvpOverride();
        if (current.isEmpty() || current.get().isLiveAt(now)) {
            return Optional.empty();
        }
        TimedPvpOverride expired = current.get();
        state = new ControlState(
            state.pvpBaseline(), Optional.empty(), state.endAccessEnabled(), state.elytraRocketsEnabled(),
            state.resistanceLevel(), state.staffAlerts()
        );
        storage.save(state);
        return Optional.of(new PvpTransition(
            expired.targetState(), state.pvpBaseline(), state.pvpBaseline(), Optional.empty(),
            expired.actor(), true
        ));
    }

    public synchronized Optional<PvpTransition> takeRecoveredExpiration() {
        Optional<PvpTransition> result = recoveredExpiration;
        recoveredExpiration = Optional.empty();
        return result;
    }

    public synchronized boolean toggleEndAccess() {
        state = new ControlState(
            state.pvpBaseline(), state.pvpOverride(), !state.endAccessEnabled(), state.elytraRocketsEnabled(),
            state.resistanceLevel(), state.staffAlerts()
        );
        storage.save(state);
        return state.endAccessEnabled();
    }

    public synchronized boolean toggleElytraRockets() {
        state = new ControlState(
            state.pvpBaseline(), state.pvpOverride(), state.endAccessEnabled(), !state.elytraRocketsEnabled(),
            state.resistanceLevel(), state.staffAlerts()
        );
        storage.save(state);
        return state.elytraRocketsEnabled();
    }

    public synchronized ResistanceLevel cycleResistance() {
        ResistanceLevel next = state.resistanceLevel().next();
        state = new ControlState(
            state.pvpBaseline(), state.pvpOverride(), state.endAccessEnabled(), state.elytraRocketsEnabled(),
            next, state.staffAlerts()
        );
        storage.save(state);
        return next;
    }

    public synchronized boolean staffAlertsEnabled(UUID playerId) {
        return state.staffAlerts().getOrDefault(playerId, true);
    }

    public synchronized boolean toggleStaffAlerts(UUID playerId) {
        Map<UUID, Boolean> alerts = new HashMap<>(state.staffAlerts());
        boolean enabled = !staffAlertsEnabled(playerId);
        alerts.put(playerId, enabled);
        state = new ControlState(
            state.pvpBaseline(), state.pvpOverride(), state.endAccessEnabled(), state.elytraRocketsEnabled(),
            state.resistanceLevel(), alerts
        );
        storage.save(state);
        return enabled;
    }
}
