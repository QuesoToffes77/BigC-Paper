package dev.linqfy.bigCasares.modules.specialitems;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class TrackerCompassService {

    private final long trackingMillis;
    private final long cooldownMillis;
    private final Map<UUID, TrackerCompassState> states = new HashMap<>();

    public TrackerCompassService(long trackingMillis, long cooldownMillis) {
        if (trackingMillis <= 0L || cooldownMillis <= 0L) {
            throw new IllegalArgumentException("tracking and cooldown durations must be positive");
        }
        this.trackingMillis = trackingMillis;
        this.cooldownMillis = cooldownMillis;
    }

    public boolean startTracking(UUID attacker, UUID target, long nowMillis) {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(target, "target");
        if (state(attacker, nowMillis) != null) {
            return false;
        }
        states.put(attacker, TrackerCompassState.tracking(target, nowMillis + trackingMillis));
        return true;
    }

    public TrackerCompassState state(UUID attacker, long nowMillis) {
        Objects.requireNonNull(attacker, "attacker");
        TrackerCompassState current = states.get(attacker);
        if (current == null) {
            return null;
        }
        if (nowMillis < current.deadlineMillis()) {
            return current;
        }
        if (current.phase() == TrackerCompassState.Phase.TRACKING) {
            TrackerCompassState cooldown = TrackerCompassState.cooldown(
                current.deadlineMillis() + cooldownMillis);
            if (nowMillis < cooldown.deadlineMillis()) {
                states.put(attacker, cooldown);
                return cooldown;
            }
        }
        states.remove(attacker);
        return null;
    }

    public void invalidateTarget(UUID target, long nowMillis) {
        Objects.requireNonNull(target, "target");
        for (Map.Entry<UUID, TrackerCompassState> entry : states.entrySet()) {
            TrackerCompassState current = entry.getValue();
            if (current.phase() == TrackerCompassState.Phase.TRACKING && target.equals(current.targetId())) {
                entry.setValue(TrackerCompassState.cooldown(nowMillis + cooldownMillis));
            }
        }
    }

    public void clear() {
        states.clear();
    }

    public int size() {
        return states.size();
    }
}
