package dev.linqfy.bigCasares.modules.servercontrol;

import java.time.Duration;
import java.util.Optional;

public record PvpTransition(
    boolean previousState,
    boolean effectiveState,
    boolean baselineState,
    Optional<Duration> duration,
    String actor,
    boolean expired
) {
}
