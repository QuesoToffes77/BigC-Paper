package dev.linqfy.bigCasares.modules.missions;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MissionPlayerState(
    UUID playerId,
    Instant generatedAt,
    Instant dailyResetsAt,
    Instant weeklyResetsAt,
    Map<String, MissionAssignment> dailyAssignments,
    Map<String, MissionAssignment> weeklyAssignments,
    Instant dailyRerolledAt
) {
}
