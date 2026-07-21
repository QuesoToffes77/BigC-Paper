package dev.linqfy.bigCasares.modules.specialitems;

import java.util.UUID;

public record TrackerCompassState(Phase phase, UUID targetId, long deadlineMillis) {

    public enum Phase {
        TRACKING,
        COOLDOWN
    }

    public static TrackerCompassState tracking(UUID targetId, long deadlineMillis) {
        return new TrackerCompassState(Phase.TRACKING, java.util.Objects.requireNonNull(targetId), deadlineMillis);
    }

    public static TrackerCompassState cooldown(long deadlineMillis) {
        return new TrackerCompassState(Phase.COOLDOWN, null, deadlineMillis);
    }
}
