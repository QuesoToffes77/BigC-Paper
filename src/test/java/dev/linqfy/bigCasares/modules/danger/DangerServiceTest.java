package dev.linqfy.bigCasares.modules.danger;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DangerServiceTest {

    @Test
    void appliesExponentialDecayWithOneTierCapPerAbsence() {
        UUID player = UUID.randomUUID();
        MemoryStorage storage = new MemoryStorage();
        storage.save(new DangerState(player, 85.0, Instant.parse("2026-07-01T00:00:00Z"), 3, Map.of()));
        DangerService service = new DangerService(
            storage, ignored -> 0.0, Clock.fixed(Instant.parse("2026-07-31T00:00:00Z"), ZoneOffset.UTC)
        );

        DangerSnapshot snapshot = service.beginSession(player);

        assertEquals(45.0, snapshot.activityScore());
        assertEquals(DangerTier.PELIGROSO, snapshot.tier());
    }

    @Test
    void repeatedVictimKillsUseDiminishingMultipliers() {
        UUID killer = UUID.randomUUID();
        UUID victim = UUID.randomUUID();
        DangerService service = new DangerService(new MemoryStorage(), ignored -> 0.0,
            Clock.fixed(Instant.parse("2026-07-17T00:00:00Z"), ZoneOffset.UTC));

        assertEquals(8.0, service.recordKill(killer, victim).awardedActivity());
        assertEquals(4.0, service.recordKill(killer, victim).awardedActivity());
        assertEquals(2.0, service.recordKill(killer, victim).awardedActivity());
        assertEquals(0.8, service.recordKill(killer, victim).awardedActivity());
    }

    private static final class MemoryStorage implements DangerStorage {
        private final Map<UUID, DangerState> states = new LinkedHashMap<>();

        @Override public Optional<DangerState> load(UUID playerId) { return Optional.ofNullable(states.get(playerId)); }
        @Override public void save(DangerState state) { states.put(state.playerId(), state); }
        @Override public java.util.List<DangerState> loadAll() { return java.util.List.copyOf(states.values()); }
    }
}
