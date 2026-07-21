package dev.linqfy.bigCasares.modules.danger;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DangerState(
    UUID playerId,
    double activityScore,
    Instant lastActiveAt,
    int departureTier,
    Map<UUID, List<Instant>> repeatedKills
) {
    public DangerState {
        activityScore = Math.max(0.0, Math.min(85.0, activityScore));
        repeatedKills = repeatedKills.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
            Map.Entry::getKey, entry -> List.copyOf(entry.getValue())
        ));
    }

    public static DangerState initial(UUID playerId, Instant now) {
        return new DangerState(playerId, 0.0, now, 1, Map.of());
    }
}
