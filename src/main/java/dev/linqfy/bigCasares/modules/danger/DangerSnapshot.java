package dev.linqfy.bigCasares.modules.danger;

import java.util.UUID;

public record DangerSnapshot(
    UUID playerId,
    double activityScore,
    double skillContribution,
    double totalScore,
    DangerTier tier,
    long inactiveDaysApplied
) {
}
