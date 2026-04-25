package dev.linqfy.bigCasares.modules.customcrossbow;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EchoShardCooldownService {

    private final int cooldownTicks;
    private final Map<UUID, Long> usableAtTick = new HashMap<>();

    public EchoShardCooldownService(int cooldownTicks) {
        this.cooldownTicks = Math.max(0, cooldownTicks);
    }

    public boolean tryUse(UUID playerId, long currentTick) {
        if (remainingTicks(playerId, currentTick) > 0) {
            return false;
        }
        usableAtTick.put(playerId, currentTick + cooldownTicks);
        return true;
    }

    public long remainingTicks(UUID playerId, long currentTick) {
        Long usableAt = usableAtTick.get(playerId);
        if (usableAt == null) {
            return 0L;
        }
        long remaining = Math.max(0L, usableAt - currentTick);
        if (remaining == 0L) {
            usableAtTick.remove(playerId);
        }
        return remaining;
    }
}
