package dev.linqfy.bigCasares.modules.loot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Bounded idempotency guard for death-event rewards. */
public final class DeathLootGuard {

    private final int maximumEntries;
    private final LinkedHashMap<UUID, Boolean> processed = new LinkedHashMap<>();

    public DeathLootGuard(int maximumEntries) {
        this.maximumEntries = Math.max(1, maximumEntries);
    }

    public synchronized boolean markIfNew(UUID entityId) {
        if (entityId == null || processed.containsKey(entityId)) {
            return false;
        }
        processed.put(entityId, Boolean.TRUE);
        while (processed.size() > maximumEntries) {
            UUID oldest = processed.keySet().iterator().next();
            processed.remove(oldest);
        }
        return true;
    }

    public synchronized void clear() {
        processed.clear();
    }
}
