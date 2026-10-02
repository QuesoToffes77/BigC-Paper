package dev.linqfy.bigCasares.modules.glider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class GliderSessionStore {

    private final Map<UUID, GliderSession> sessions = new HashMap<>();

    public GliderSession start(UUID playerId, GliderTier tier, long startTick, double startY) {
        return start(playerId, null, tier, startTick, startY);
    }

    public GliderSession start(UUID playerId, UUID worldId, GliderTier tier, long startTick, double startY) {
        GliderSession session = new GliderSession(playerId, worldId, tier, startTick, startY);
        sessions.put(playerId, session);
        return session;
    }

    public Optional<GliderSession> find(UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }

    public boolean isActive(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public Optional<GliderSession> stop(UUID playerId, GliderStopReason reason) {
        GliderSession removed = sessions.remove(playerId);
        if (removed != null) {
            removed.finish(reason);
        }
        return Optional.ofNullable(removed);
    }

    public Collection<GliderSession> activeSessions() {
        return java.util.List.copyOf(sessions.values());
    }

    public int size() {
        return sessions.size();
    }

    public void clear() {
        for (GliderSession session : sessions.values()) {
            session.finish(GliderStopReason.MODULE_DISABLE);
        }
        sessions.clear();
    }
}
