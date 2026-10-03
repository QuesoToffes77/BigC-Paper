package dev.linqfy.bigCasares.modules.glider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GliderBoostCooldown {

    private final Map<UUID, Long> lastUseTick = new HashMap<>();

    public boolean tryUse(UUID playerId, long currentTick, int cooldownTicks) {
        Long lastUse = lastUseTick.get(playerId);
        if (lastUse != null && currentTick - lastUse < Math.max(1, cooldownTicks)) {
            return false;
        }
        lastUseTick.put(playerId, currentTick);
        return true;
    }

    public boolean tryUse(UUID playerId, long currentTick, GliderTierStats stats) {
        return stats.boostEnabled() && tryUse(playerId, currentTick, stats.boostCooldownTicks());
    }

    public void prune(long currentTick) {
        lastUseTick.entrySet().removeIf(entry -> currentTick - entry.getValue() > 1_200L);
    }

    public void clear() {
        lastUseTick.clear();
    }
}
