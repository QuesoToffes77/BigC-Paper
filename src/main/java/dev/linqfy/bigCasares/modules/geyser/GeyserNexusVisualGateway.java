package dev.linqfy.bigCasares.modules.geyser;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GeyserNexusVisualGateway {
    private final Map<UUID, String> states = new ConcurrentHashMap<>();

    public void update(UUID nexusId, String state) {
        states.put(nexusId, state);
    }

    public String state(UUID nexusId) {
        return states.getOrDefault(nexusId, "idle");
    }

    public void remove(UUID nexusId) {
        states.remove(nexusId);
    }
}
