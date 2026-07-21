package dev.linqfy.bigCasares.modules.danger;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

public final class DangerService {
    private static final Duration REPEAT_WINDOW = Duration.ofHours(24);
    private static final double[] REPEAT_MULTIPLIERS = {1.0, 0.5, 0.25, 0.1};

    private final DangerStorage storage;
    private final ToDoubleFunction<UUID> skillRating;
    private final Clock clock;

    public DangerService(DangerStorage storage, ToDoubleFunction<UUID> skillRating, Clock clock) {
        this.storage = storage;
        this.skillRating = skillRating;
        this.clock = clock;
    }

    public synchronized DangerSnapshot beginSession(UUID playerId) {
        Instant now = clock.instant();
        DangerState state = state(playerId, now);
        long days = Math.max(0L, Duration.between(state.lastActiveAt(), now).toDays());
        if (days == 0L) {
            DangerState active = withActivityAndTime(state, state.activityScore(), now, state.departureTier());
            storage.save(active);
            return snapshot(active, 0L);
        }
        double contribution = skillContribution(playerId);
        double candidate = roundOneDecimal(state.activityScore() * Math.pow(0.90, days));
        DangerTier departure = DangerTier.fromLevel(state.departureTier());
        double minimumTotal = departure == DangerTier.LETAL ? DangerTier.PELIGROSO.minimum() : 0.0;
        double minimumActivity = Math.max(0.0, minimumTotal - contribution);
        DangerState decayed = withActivityAndTime(state, Math.max(candidate, minimumActivity), now, departure.level());
        storage.save(decayed);
        return snapshot(decayed, days);
    }

    public synchronized DangerSnapshot touch(UUID playerId) {
        Instant now = clock.instant();
        DangerState state = state(playerId, now);
        DangerSnapshot current = snapshot(state, 0L);
        DangerState touched = withActivityAndTime(state, state.activityScore(), now, current.tier().level());
        storage.save(touched);
        return snapshot(touched, 0L);
    }

    public synchronized DangerKillResult recordKill(UUID killerId, UUID victimId) {
        Instant now = clock.instant();
        DangerState killer = state(killerId, now);
        Map<UUID, List<Instant>> history = mutableHistory(killer);
        List<Instant> recent = new ArrayList<>(history.getOrDefault(victimId, List.of()));
        recent.removeIf(kill -> kill.isBefore(now.minus(REPEAT_WINDOW)));
        double multiplier = REPEAT_MULTIPLIERS[Math.min(recent.size(), REPEAT_MULTIPLIERS.length - 1)];
        double awarded = roundOneDecimal(8.0 * multiplier);
        recent.add(now);
        history.put(victimId, List.copyOf(recent));
        DangerState updated = new DangerState(
            killerId, killer.activityScore() + awarded, killer.lastActiveAt(), killer.departureTier(), history
        );
        storage.save(updated);
        return new DangerKillResult(awarded, snapshot(updated, 0L));
    }

    public synchronized DangerDeathResult recordDeath(UUID victimId, UUID killerId) {
        Instant now = clock.instant();
        DangerState victim = state(victimId, now);
        DangerTier victimTier = snapshot(victim, 0L).tier();
        DangerTier killerTier = killerId == null ? victimTier : snapshot(state(killerId, now), 0L).tier();
        double loss = 3.0;
        if (killerTier.level() < victimTier.level()) {
            loss += (victimTier.level() - killerTier.level()) * 2.0;
        }
        if (victimTier == DangerTier.LETAL) {
            loss += 2.0;
        }
        DangerState updated = withActivityAndTime(
            victim, victim.activityScore() - loss, victim.lastActiveAt(), victim.departureTier()
        );
        storage.save(updated);
        return new DangerDeathResult(loss, victimTier, snapshot(updated, 0L));
    }

    public synchronized DangerSnapshot awardBossPlacement(UUID playerId, int placement) {
        double award = switch (placement) { case 1 -> 6.0; case 2 -> 4.0; case 3 -> 2.0; default -> 0.0; };
        return changeActivity(playerId, award);
    }

    public synchronized DangerSnapshot snapshot(UUID playerId) {
        return snapshot(state(playerId, clock.instant()), 0L);
    }

    public synchronized List<DangerSnapshot> top(int limit) {
        return storage.loadAll().stream().map(state -> snapshot(state, 0L))
            .sorted(Comparator.comparingDouble(DangerSnapshot::totalScore).reversed()).limit(limit).toList();
    }

    private DangerSnapshot changeActivity(UUID playerId, double delta) {
        DangerState state = state(playerId, clock.instant());
        DangerState updated = withActivityAndTime(
            state, state.activityScore() + delta, state.lastActiveAt(), state.departureTier()
        );
        storage.save(updated);
        return snapshot(updated, 0L);
    }

    private DangerSnapshot snapshot(DangerState state, long inactiveDays) {
        double contribution = skillContribution(state.playerId());
        double total = Math.max(0.0, Math.min(100.0, state.activityScore() + contribution));
        return new DangerSnapshot(
            state.playerId(), state.activityScore(), contribution, roundOneDecimal(total),
            DangerTier.fromScore(total), inactiveDays
        );
    }

    private double skillContribution(UUID playerId) {
        return Math.max(0.0, Math.min(15.0, skillRating.applyAsDouble(playerId) / 3500.0 * 15.0));
    }

    private DangerState state(UUID playerId, Instant now) {
        return storage.load(playerId).orElseGet(() -> DangerState.initial(playerId, now));
    }

    private DangerState withActivityAndTime(DangerState state, double activity, Instant time, int departureTier) {
        return new DangerState(state.playerId(), activity, time, departureTier, state.repeatedKills());
    }

    private Map<UUID, List<Instant>> mutableHistory(DangerState state) {
        Map<UUID, List<Instant>> result = new LinkedHashMap<>();
        state.repeatedKills().forEach((id, entries) -> result.put(id, new ArrayList<>(entries)));
        return result;
    }

    private static double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
