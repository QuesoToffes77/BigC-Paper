package dev.linqfy.bigCasares.modules.smokebomb;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SmokeCloudService {

    private final SmokeBombSettings settings;
    private final Map<String, Long> activeClouds = new LinkedHashMap<>();
    private final Set<UUID> concealedEntities = new LinkedHashSet<>();
    private final Map<UUID, Long> pendingRevealTicks = new LinkedHashMap<>();

    public SmokeCloudService(SmokeBombSettings settings) {
        this.settings = settings;
    }

    public void createCloud(String cloudId, long createdAtTick) {
        activeClouds.put(cloudId, createdAtTick + settings.durationTicks());
    }

    public TickResult tick(long currentTick, Set<UUID> occupantsInActiveClouds) {
        expireClouds(currentTick);

        Set<UUID> effectiveOccupants = activeClouds.isEmpty()
            ? Set.of()
            : new LinkedHashSet<>(occupantsInActiveClouds);

        Set<UUID> newlyConcealed = new LinkedHashSet<>();
        Set<UUID> newlyRevealed = new LinkedHashSet<>();

        for (UUID occupantId : effectiveOccupants) {
            pendingRevealTicks.remove(occupantId);
            if (concealedEntities.add(occupantId)) {
                newlyConcealed.add(occupantId);
            }
        }

        for (UUID concealedEntityId : new LinkedHashSet<>(concealedEntities)) {
            if (effectiveOccupants.contains(concealedEntityId)) {
                continue;
            }
            pendingRevealTicks.putIfAbsent(concealedEntityId, currentTick + settings.lingerTicks());
        }

        for (Map.Entry<UUID, Long> entry : new LinkedHashMap<>(pendingRevealTicks).entrySet()) {
            UUID entityId = entry.getKey();
            long revealAtTick = entry.getValue();
            if (effectiveOccupants.contains(entityId) || revealAtTick > currentTick) {
                continue;
            }
            pendingRevealTicks.remove(entityId);
            if (concealedEntities.remove(entityId)) {
                newlyRevealed.add(entityId);
            }
        }

        return new TickResult(
            Collections.unmodifiableSet(newlyConcealed),
            Collections.unmodifiableSet(newlyRevealed)
        );
    }

    public Set<UUID> getConcealedEntities() {
        return Collections.unmodifiableSet(concealedEntities);
    }

    private void expireClouds(long currentTick) {
        activeClouds.entrySet().removeIf(entry -> currentTick >= entry.getValue());
    }

    public record TickResult(Set<UUID> newlyConcealed, Set<UUID> newlyRevealed) {
    }
}
