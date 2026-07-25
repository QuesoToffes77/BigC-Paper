package dev.linqfy.bigCasares.modules.endevent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class EndEventService {
    private static final Duration COMBAT_DURATION = Duration.ofSeconds(60);
    private static final int ALLOWED_DISCONNECTS = 3;

    private final EndEventStorage storage;
    private final Clock clock;
    private EndEventPhase phase;
    private Instant huntStartedAt;
    private UUID eggCarrier;
    private final Map<UUID, EndEventParticipantSnapshot> participants = new LinkedHashMap<>();
    private final Set<EndEventMilestone> claimedMilestones = new LinkedHashSet<>();

    public EndEventService(EndEventStorage storage, Clock clock) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.clock = Objects.requireNonNull(clock, "clock");
        restore(storage.load().orElseGet(EndEventSnapshot::armed));
    }

    public synchronized void beginHunt(Set<UUID> participantIds, UUID initialHunter) {
        Objects.requireNonNull(participantIds, "participantIds");
        if (!participantIds.contains(initialHunter)) {
            throw new IllegalArgumentException("Initial hunter must be a participant");
        }
        participants.clear();
        participantIds.forEach(id -> participants.put(id, EndEventParticipantSnapshot.active()));
        claimedMilestones.clear();
        huntStartedAt = clock.instant();
        eggCarrier = initialHunter;
        phase = EndEventPhase.HUNT;
        persist();
    }

    public synchronized EndEventDeathResult recordDeath(UUID victim, Optional<UUID> killer) {
        EndEventParticipantSnapshot state = participants.get(victim);
        if (state == null || state.eliminated() || phase != EndEventPhase.HUNT) {
            return EndEventDeathResult.ignored();
        }
        boolean victimWasHunter = victim.equals(eggCarrier);
        boolean killedByHunter = killer.filter(id -> id.equals(eggCarrier)).isPresent();
        boolean eliminated = victimWasHunter || killedByHunter;
        if (eliminated) {
            eliminate(victim);
        }
        return new EndEventDeathResult(true, eliminated, killedByHunter);
    }

    public synchronized void recordCombat(UUID first, UUID second) {
        if (phase != EndEventPhase.HUNT) {
            return;
        }
        Instant until = clock.instant().plus(COMBAT_DURATION);
        tagCombat(first, until);
        tagCombat(second, until);
        persist();
    }

    public synchronized EndEventDisconnectResult disconnect(UUID playerId) {
        EndEventParticipantSnapshot current = participants.get(playerId);
        if (current == null || current.eliminated() || phase != EndEventPhase.HUNT) {
            return EndEventDisconnectResult.NOT_PARTICIPATING;
        }
        if (playerId.equals(eggCarrier)) {
            eggCarrier = null;
        }
        if (current.combatUntil().map(until -> until.isAfter(clock.instant())).orElse(false)) {
            participants.put(playerId, current.withOnline(false).withEliminated(true));
            persist();
            return EndEventDisconnectResult.ELIMINATED_COMBAT_LOG;
        }
        int disconnects = current.disconnects() + 1;
        if (disconnects > ALLOWED_DISCONNECTS) {
            participants.put(playerId, current.withOnline(false)
                .withDisconnects(disconnects).withEliminated(true));
            persist();
            return EndEventDisconnectResult.ELIMINATED_DISCONNECT_LIMIT;
        }
        participants.put(playerId, current.withOnline(false).withDisconnects(disconnects));
        persist();
        return EndEventDisconnectResult.GRACE;
    }

    public synchronized boolean reconnect(UUID playerId) {
        EndEventParticipantSnapshot current = participants.get(playerId);
        if (current == null || current.eliminated()) {
            return false;
        }
        participants.put(playerId, current.withOnline(true));
        persist();
        return true;
    }

    public synchronized void eliminate(UUID playerId) {
        EndEventParticipantSnapshot current = participants.get(playerId);
        if (current == null || current.eliminated()) {
            return;
        }
        participants.put(playerId, current.withEliminated(true));
        if (playerId.equals(eggCarrier)) {
            eggCarrier = null;
        }
        persist();
    }

    public synchronized Set<EndEventMilestone> claimDueMilestones() {
        if (phase != EndEventPhase.HUNT || huntStartedAt == null) {
            return Set.of();
        }
        Instant now = clock.instant();
        Set<EndEventMilestone> due = new LinkedHashSet<>();
        for (EndEventMilestone milestone : EndEventMilestone.values()) {
            if (!claimedMilestones.contains(milestone)
                && !huntStartedAt.plus(milestone.offset()).isAfter(now)) {
                claimedMilestones.add(milestone);
                due.add(milestone);
            }
        }
        if (!due.isEmpty()) {
            persist();
        }
        return Set.copyOf(due);
    }

    public synchronized boolean hasClaimed(EndEventMilestone milestone) {
        return claimedMilestones.contains(milestone);
    }

    public synchronized Set<EndEventMilestone> claimedMilestones() {
        return Set.copyOf(claimedMilestones);
    }

    public synchronized void setEggCarrier(Optional<UUID> playerId) {
        UUID next = playerId.orElse(null);
        if (next != null && !isSurvivor(next)) {
            throw new IllegalArgumentException("Egg carrier must be a surviving participant");
        }
        eggCarrier = next;
        persist();
    }

    public synchronized Optional<UUID> eggCarrier() {
        return Optional.ofNullable(eggCarrier);
    }

    public synchronized boolean isParticipant(UUID playerId) {
        return participants.containsKey(playerId);
    }

    public synchronized boolean isSurvivor(UUID playerId) {
        EndEventParticipantSnapshot participant = participants.get(playerId);
        return participant != null && !participant.eliminated();
    }

    public synchronized boolean isOnline(UUID playerId) {
        EndEventParticipantSnapshot participant = participants.get(playerId);
        return participant != null && participant.online();
    }

    public synchronized Set<UUID> survivors() {
        Set<UUID> survivors = new LinkedHashSet<>();
        participants.forEach((id, state) -> {
            if (!state.eliminated()) {
                survivors.add(id);
            }
        });
        return Set.copyOf(survivors);
    }

    public synchronized Optional<UUID> onlineWinner() {
        Set<UUID> survivors = survivors();
        if (survivors.size() != 1) {
            return Optional.empty();
        }
        UUID winner = survivors.iterator().next();
        return isOnline(winner) ? Optional.of(winner) : Optional.empty();
    }

    public synchronized Optional<Instant> combatUntil(UUID playerId) {
        EndEventParticipantSnapshot participant = participants.get(playerId);
        if (participant == null) {
            return Optional.empty();
        }
        return participant.combatUntil().filter(until -> until.isAfter(clock.instant()));
    }

    public synchronized Optional<Instant> huntStartedAt() {
        return Optional.ofNullable(huntStartedAt);
    }

    public synchronized EndEventPhase phase() {
        return phase;
    }

    public synchronized EndEventSnapshot snapshot() {
        return new EndEventSnapshot(
            phase,
            Optional.ofNullable(huntStartedAt),
            Optional.ofNullable(eggCarrier),
            participants,
            claimedMilestones
        );
    }

    public synchronized void setPhase(EndEventPhase next) {
        phase = Objects.requireNonNull(next, "next");
        persist();
    }

    public synchronized void reset() {
        phase = EndEventPhase.ARMED;
        huntStartedAt = null;
        eggCarrier = null;
        participants.clear();
        claimedMilestones.clear();
        persist();
    }

    public synchronized void debugSetElapsed(Duration elapsed) {
        if (phase != EndEventPhase.HUNT) {
            throw new IllegalStateException("The hunt is not active");
        }
        if (elapsed == null || elapsed.isNegative()) {
            throw new IllegalArgumentException("Elapsed time cannot be negative");
        }
        huntStartedAt = clock.instant().minus(elapsed);
        claimedMilestones.clear();
        persist();
    }

    private void tagCombat(UUID playerId, Instant until) {
        EndEventParticipantSnapshot current = participants.get(playerId);
        if (current != null && !current.eliminated()) {
            participants.put(playerId, current.withCombatUntil(Optional.of(until)));
        }
    }

    private void restore(EndEventSnapshot snapshot) {
        phase = snapshot.phase();
        huntStartedAt = snapshot.huntStartedAt().orElse(null);
        eggCarrier = snapshot.eggCarrier().orElse(null);
        participants.clear();
        participants.putAll(snapshot.participants());
        claimedMilestones.clear();
        claimedMilestones.addAll(snapshot.claimedMilestones());
    }

    private void persist() {
        storage.save(snapshot());
    }
}
