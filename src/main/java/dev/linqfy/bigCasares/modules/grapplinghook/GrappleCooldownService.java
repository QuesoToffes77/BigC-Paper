package dev.linqfy.bigCasares.modules.grapplinghook;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player wall-clock cooldown state. Pure: the caller injects the current
 * time so behavior is unit-testable without a server.
 */
public final class GrappleCooldownService {

    private final Map<UUID, Long> cooldownUntilMillis = new HashMap<>();

    public boolean isOnCooldown(UUID playerId, long nowMillis) {
        if (playerId == null) {
            return true;
        }
        Long until = cooldownUntilMillis.get(playerId);
        return until != null && until > nowMillis;
    }

    /** Starts a cooldown of {@code durationMillis}, extending any active one. */
    public void start(UUID playerId, long durationMillis, long nowMillis) {
        if (playerId == null || durationMillis <= 0) {
            return;
        }
        cooldownUntilMillis.merge(playerId, nowMillis + durationMillis, Math::max);
    }

    public long remainingMillis(UUID playerId, long nowMillis) {
        if (playerId == null) {
            return 0L;
        }
        Long until = cooldownUntilMillis.get(playerId);
        return until == null ? 0L : Math.max(0L, until - nowMillis);
    }

    public int activeCount() {
        return cooldownUntilMillis.size();
    }

    public void clear() {
        cooldownUntilMillis.clear();
    }
}
