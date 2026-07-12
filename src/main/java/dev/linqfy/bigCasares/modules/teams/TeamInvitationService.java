package dev.linqfy.bigCasares.modules.teams;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class TeamInvitationService {

    private final Clock clock;
    private final Duration lifetime;
    private final Map<InvitationKey, Instant> expiresAtByInvitation = new HashMap<>();

    public TeamInvitationService(Clock clock, Duration lifetime) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.lifetime = Objects.requireNonNull(lifetime, "lifetime");
        if (lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException("invitation lifetime must be positive");
        }
    }

    public synchronized void invite(TeamId teamId, UUID playerId) {
        InvitationKey key = new InvitationKey(teamId, playerId);
        expiresAtByInvitation.put(key, clock.instant().plus(lifetime));
    }

    public synchronized boolean hasValidInvitation(TeamId teamId, UUID playerId) {
        InvitationKey key = new InvitationKey(teamId, playerId);
        Instant expiresAt = expiresAtByInvitation.get(key);
        if (expiresAt == null) {
            return false;
        }
        if (!clock.instant().isBefore(expiresAt)) {
            expiresAtByInvitation.remove(key);
            return false;
        }
        return true;
    }

    public synchronized boolean consume(TeamId teamId, UUID playerId) {
        if (!hasValidInvitation(teamId, playerId)) {
            return false;
        }
        expiresAtByInvitation.remove(new InvitationKey(teamId, playerId));
        return true;
    }

    public synchronized void clearPlayer(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        expiresAtByInvitation.keySet().removeIf(key -> key.playerId().equals(playerId));
    }

    public synchronized void clearTeam(TeamId teamId) {
        Objects.requireNonNull(teamId, "teamId");
        expiresAtByInvitation.keySet().removeIf(key -> key.teamId().equals(teamId));
    }

    private record InvitationKey(TeamId teamId, UUID playerId) {
        private InvitationKey {
            Objects.requireNonNull(teamId, "teamId");
            Objects.requireNonNull(playerId, "playerId");
        }
    }
}
