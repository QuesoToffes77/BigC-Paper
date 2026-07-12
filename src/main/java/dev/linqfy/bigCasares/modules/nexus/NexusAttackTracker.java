package dev.linqfy.bigCasares.modules.nexus;

import java.util.*;
import java.util.function.Predicate;

public final class NexusAttackTracker {
    private final Map<NexusId, Set<UUID>> attackers = new HashMap<>();

    public synchronized boolean register(NexusId nexusId, UUID attackerId) {
        Set<UUID> set = attackers.computeIfAbsent(nexusId, ignored -> new HashSet<>());
        boolean wasEmpty = set.isEmpty();
        set.add(attackerId);
        return wasEmpty;
    }

    public synchronized boolean retain(NexusId nexusId, Predicate<UUID> active) {
        Set<UUID> values = attackers.get(nexusId);
        if (values == null) return false;
        values.removeIf(active.negate());
        if (values.isEmpty()) attackers.remove(nexusId);
        return !values.isEmpty();
    }

    public synchronized void clear(NexusId nexusId) { attackers.remove(nexusId); }
}
