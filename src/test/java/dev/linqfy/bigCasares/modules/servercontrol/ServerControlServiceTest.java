package dev.linqfy.bigCasares.modules.servercontrol;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ServerControlServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-15T12:00:00Z");

    @Test
    void timedOverrideAppliesImmediatelyAndRestoresOriginalBaseline() {
        InMemoryControlStorage storage = new InMemoryControlStorage(ControlState.defaults());
        ServerControlService service = new ServerControlService(storage, fixedClock(NOW));

        PvpTransition started = service.setTimedPvp(false, Duration.ofMinutes(10), "Admin");

        assertFalse(started.effectiveState());
        assertTrue(service.state().pvpBaseline());
        assertEquals(NOW.plus(Duration.ofMinutes(10)), service.state().pvpOverride().orElseThrow().expiresAt());

        Optional<PvpTransition> expired = service.expirePvpOverride(NOW.plus(Duration.ofMinutes(11)));

        assertTrue(expired.orElseThrow().effectiveState());
        assertTrue(expired.orElseThrow().expired());
        assertTrue(service.state().pvpOverride().isEmpty());
    }

    @Test
    void replacingTimedOverrideRetainsOriginalBaseline() {
        ServerControlService service = new ServerControlService(
            new InMemoryControlStorage(ControlState.defaults()), fixedClock(NOW)
        );

        service.setTimedPvp(false, Duration.ofMinutes(5), "Admin");
        service.setTimedPvp(true, Duration.ofMinutes(20), "OtherAdmin");

        assertTrue(service.state().pvpBaseline());
        assertTrue(service.effectivePvp(NOW.plusSeconds(1)));
        assertEquals(NOW.plus(Duration.ofMinutes(20)), service.state().pvpOverride().orElseThrow().expiresAt());
    }

    @Test
    void permanentChoiceClearsOverrideAndChangesBaseline() {
        ServerControlService service = new ServerControlService(
            new InMemoryControlStorage(ControlState.defaults()), fixedClock(NOW)
        );
        service.setTimedPvp(false, Duration.ofMinutes(10), "Admin");

        service.setPermanentPvp(false, "Admin");

        assertFalse(service.state().pvpBaseline());
        assertTrue(service.state().pvpOverride().isEmpty());
    }

    @Test
    void constructorExpiresAnOverrideThatElapsedWhileOffline() {
        ControlState persisted = new ControlState(
            true,
            Optional.of(new TimedPvpOverride(false, NOW.minusSeconds(1), "Admin")),
            true,
            true,
            ResistanceLevel.OFF,
            java.util.Map.of()
        );
        InMemoryControlStorage storage = new InMemoryControlStorage(persisted);

        ServerControlService service = new ServerControlService(storage, fixedClock(NOW));

        assertTrue(service.effectivePvp(NOW));
        assertTrue(service.state().pvpOverride().isEmpty());
        assertTrue(service.takeRecoveredExpiration().orElseThrow().expired());
        assertTrue(service.takeRecoveredExpiration().isEmpty());
        assertEquals(1, storage.saveCount);
    }

    @Test
    void cyclesResistanceAndDefaultsOperatorAlertsToEnabled() {
        UUID operator = UUID.randomUUID();
        ServerControlService service = new ServerControlService(
            new InMemoryControlStorage(ControlState.defaults()), fixedClock(NOW)
        );

        assertTrue(service.staffAlertsEnabled(operator));
        assertEquals(ResistanceLevel.I, service.cycleResistance());
        assertEquals(ResistanceLevel.II, service.cycleResistance());
        assertEquals(ResistanceLevel.OFF, service.cycleResistance());
        assertFalse(service.toggleStaffAlerts(operator));
    }

    private static Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private static final class InMemoryControlStorage implements ControlStorage {
        private ControlState state;
        private int saveCount;

        private InMemoryControlStorage(ControlState state) {
            this.state = state;
        }

        @Override
        public ControlState load() {
            return state;
        }

        @Override
        public void save(ControlState state) {
            this.state = state;
            saveCount++;
        }
    }
}
