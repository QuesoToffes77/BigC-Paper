package dev.linqfy.bigCasares.modules.geyser;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GeyserBossVisualGateway {
    private final Map<UUID, String> phases = new ConcurrentHashMap<>();

    public void updatePhase(UUID bossInstanceId, String phase) {
        phases.put(bossInstanceId, phase);
    }

    public String phase(UUID bossInstanceId) {
        return phases.getOrDefault(bossInstanceId, "phase-1");
    }

    public void remove(UUID bossInstanceId) {
        phases.remove(bossInstanceId);
    }
}
