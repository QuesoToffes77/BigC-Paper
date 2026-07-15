package dev.linqfy.bigCasares.modules.servercontrol;

import java.time.Instant;
import java.util.Objects;

public record TimedPvpOverride(boolean targetState, Instant expiresAt, String actor) {

    public TimedPvpOverride {
        Objects.requireNonNull(expiresAt, "expiresAt");
        actor = actor == null || actor.isBlank() ? "Sistema" : actor;
    }

    public boolean isLiveAt(Instant instant) {
        return expiresAt.isAfter(instant);
    }
}
