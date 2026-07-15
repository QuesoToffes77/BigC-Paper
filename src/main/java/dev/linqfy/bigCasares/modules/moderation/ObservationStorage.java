package dev.linqfy.bigCasares.modules.moderation;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public interface ObservationStorage {

    void append(Observation observation);

    boolean hasSeen(UUID playerId);

    void markSeen(UUID playerId);

    void purgeOlderThan(Instant cutoff);
}
