package dev.linqfy.bigCasares.modules.endevent;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record EndEventSnapshot(
    EndEventPhase phase,
    Optional<Instant> huntStartedAt,
    Optional<UUID> eggCarrier,
    Map<UUID, EndEventParticipantSnapshot> participants,
    Set<EndEventMilestone> claimedMilestones
) {

    public EndEventSnapshot {
        phase = phase == null ? EndEventPhase.ARMED : phase;
        huntStartedAt = huntStartedAt == null ? Optional.empty() : huntStartedAt;
        eggCarrier = eggCarrier == null ? Optional.empty() : eggCarrier;
        participants = participants == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(participants));
        claimedMilestones = claimedMilestones == null
            ? Set.of()
            : Set.copyOf(new LinkedHashSet<>(claimedMilestones));
    }

    public static EndEventSnapshot armed() {
        return new EndEventSnapshot(
            EndEventPhase.ARMED, Optional.empty(), Optional.empty(), Map.of(), Set.of()
        );
    }
}
