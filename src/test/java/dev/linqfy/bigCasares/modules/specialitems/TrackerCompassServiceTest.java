package dev.linqfy.bigCasares.modules.specialitems;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackerCompassServiceTest {

    private static final long TRACKING_MS = 45_000L;
    private static final long COOLDOWN_MS = 60_000L;

    private final UUID attacker = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();
    private final TrackerCompassService service = new TrackerCompassService(TRACKING_MS, COOLDOWN_MS);

    @Test
    void startsTrackingForAnIdleAttacker() {
        assertTrue(service.startTracking(attacker, target, 1_000L));

        TrackerCompassState state = service.state(attacker, 1_000L);
        assertEquals(TrackerCompassState.Phase.TRACKING, state.phase());
        assertEquals(target, state.targetId());
        assertEquals(46_000L, state.deadlineMillis());
    }

    @Test
    void rejectsHitsDuringTrackingWithoutChangingTheTargetOrDeadline() {
        UUID replacement = UUID.randomUUID();
        service.startTracking(attacker, target, 1_000L);

        assertFalse(service.startTracking(attacker, replacement, 2_000L));

        TrackerCompassState state = service.state(attacker, 2_000L);
        assertEquals(target, state.targetId());
        assertEquals(46_000L, state.deadlineMillis());
    }

    @Test
    void entersCooldownAtTheExactTrackingDeadline() {
        service.startTracking(attacker, target, 1_000L);

        TrackerCompassState state = service.state(attacker, 46_000L);

        assertEquals(TrackerCompassState.Phase.COOLDOWN, state.phase());
        assertNull(state.targetId());
        assertEquals(106_000L, state.deadlineMillis());
    }

    @Test
    void invalidatingTargetStartsAFullCooldownAtInvalidationTime() {
        service.startTracking(attacker, target, 1_000L);

        service.invalidateTarget(target, 9_000L);

        TrackerCompassState state = service.state(attacker, 9_000L);
        assertEquals(TrackerCompassState.Phase.COOLDOWN, state.phase());
        assertEquals(69_000L, state.deadlineMillis());
    }

    @Test
    void rejectsHitsDuringCooldownAndExpiresAtTheExactDeadline() {
        service.startTracking(attacker, target, 1_000L);
        service.state(attacker, 46_000L);

        assertFalse(service.startTracking(attacker, UUID.randomUUID(), 50_000L));
        assertNull(service.state(attacker, 106_000L));
        assertTrue(service.startTracking(attacker, UUID.randomUUID(), 106_000L));
    }

    @Test
    void clearRemovesAllTrackingAndCooldownState() {
        service.startTracking(attacker, target, 1_000L);
        service.startTracking(UUID.randomUUID(), UUID.randomUUID(), 1_000L);

        service.clear();

        assertNull(service.state(attacker, 1_000L));
        assertEquals(0, service.size());
    }
}
