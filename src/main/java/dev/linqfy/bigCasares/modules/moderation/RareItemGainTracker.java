package dev.linqfy.bigCasares.modules.moderation;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class RareItemGainTracker {
    private final Set<String> rareItems;
    private final Map<UUID, Map<String, Integer>> snapshots = new HashMap<>();
    private final Set<UUID> explained = new java.util.HashSet<>();

    public RareItemGainTracker(Set<String> rareItems) {
        this.rareItems = Set.copyOf(rareItems);
    }

    public synchronized void markExplained(UUID playerId) {
        explained.add(playerId);
    }

    public synchronized Optional<AbuseSignal> sample(
        Instant now,
        UUID playerId,
        String playerName,
        Map<String, Integer> counts
    ) {
        Map<String, Integer> current = new HashMap<>();
        for (String item : rareItems) {
            current.put(item, counts.getOrDefault(item, 0));
        }
        Map<String, Integer> previous = snapshots.put(playerId, Map.copyOf(current));
        boolean wasExplained = explained.remove(playerId);
        if (previous == null || wasExplained) {
            return Optional.empty();
        }
        for (String item : rareItems) {
            int gain = current.getOrDefault(item, 0) - previous.getOrDefault(item, 0);
            if (gain > 0) {
                return Optional.of(new AbuseSignal(
                    now, playerId, playerName, "rare-item-gain", 25,
                    "Ganancia no explicada en un segundo: " + item + " x" + gain
                ));
            }
        }
        return Optional.empty();
    }

    public synchronized void forget(UUID playerId) {
        snapshots.remove(playerId);
        explained.remove(playerId);
    }
}
