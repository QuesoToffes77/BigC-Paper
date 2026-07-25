package dev.linqfy.bigCasares.modules.endevent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndEventServiceTest {

    private static final Instant START = Instant.parse("2026-07-26T01:30:00Z");

    private UUID hunter;
    private UUID survivor;
    private UUID third;
    private MutableClock clock;
    private InMemoryStorage storage;
    private EndEventService service;

    @BeforeEach
    void setUp() {
        hunter = UUID.randomUUID();
        survivor = UUID.randomUUID();
        third = UUID.randomUUID();
        clock = new MutableClock(START);
        storage = new InMemoryStorage();
        service = new EndEventService(storage, clock);
        service.beginHunt(new LinkedHashSet<>(Set.of(hunter, survivor, third)), hunter);
    }

    @Test
    void hunterKillEliminatesVictimButNonHunterKillDoesNot() {
        EndEventDeathResult hunterKill = service.recordDeath(survivor, Optional.of(hunter));
        assertTrue(hunterKill.eliminated());
        assertTrue(hunterKill.customHunterMessage());

        service.setEggCarrier(Optional.of(third));
        EndEventDeathResult nonHunterKill = service.recordDeath(hunter, Optional.of(survivor));
        assertFalse(nonHunterKill.eliminated());
        assertFalse(nonHunterKill.customHunterMessage());
    }

    @Test
    void hunterDeathFromAnyCauseEliminatesHunter() {
        EndEventDeathResult result = service.recordDeath(hunter, Optional.empty());

        assertTrue(result.eliminated());
        assertEquals(Optional.empty(), service.eggCarrier());
        assertEquals(Set.of(survivor, third), service.survivors());
    }

    @Test
    void combatLogoutEliminatesImmediately() {
        service.recordCombat(hunter, survivor);

        EndEventDisconnectResult result = service.disconnect(survivor);

        assertEquals(EndEventDisconnectResult.ELIMINATED_COMBAT_LOG, result);
        assertFalse(service.isSurvivor(survivor));
    }

    @Test
    void firstThreeNonCombatDisconnectsHaveUnlimitedGraceAndFourthEliminates() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            assertEquals(EndEventDisconnectResult.GRACE, service.disconnect(survivor));
            assertTrue(service.isSurvivor(survivor));
            assertFalse(service.isOnline(survivor));
            service.reconnect(survivor);
        }

        assertEquals(EndEventDisconnectResult.ELIMINATED_DISCONNECT_LIMIT, service.disconnect(survivor));
        assertFalse(service.isSurvivor(survivor));
    }

    @Test
    void milestonesAreClaimedOnceFromAbsoluteHuntTime() {
        clock.advanceSeconds(15 * 60);

        assertEquals(Set.of(
            EndEventMilestone.POTIONS_LOCKED,
            EndEventMilestone.NETHER_WARNING,
            EndEventMilestone.NETHER_LOCKED,
            EndEventMilestone.END_WARNING,
            EndEventMilestone.END_LOCKED
        ), service.claimDueMilestones());
        assertTrue(service.claimDueMilestones().isEmpty());
    }

    @Test
    void offlineOnlySurvivorDoesNotBecomeWinnerUntilReconnect() {
        service.eliminate(hunter);
        service.eliminate(third);
        service.disconnect(survivor);

        assertEquals(Optional.empty(), service.onlineWinner());

        service.reconnect(survivor);
        assertEquals(Optional.of(survivor), service.onlineWinner());
    }

    private static final class InMemoryStorage implements EndEventStorage {
        private EndEventSnapshot value;

        @Override
        public Optional<EndEventSnapshot> load() {
            return Optional.ofNullable(value);
        }

        @Override
        public void save(EndEventSnapshot snapshot) {
            value = snapshot;
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
