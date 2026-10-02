package dev.linqfy.bigCasares.modules.grapplinghook;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrapplingHookInputGateTest {

    @Test
    void onePhysicalClickCanOnlyCreateOneActivation() {
        GrapplingHookInputGate gate = new GrapplingHookInputGate(50L);
        UUID player = UUID.randomUUID();

        assertTrue(gate.tryAcquire(player, 1_000L));
        assertFalse(gate.tryAcquire(player, 1_010L));
        assertTrue(gate.tryAcquire(player, 1_050L));
    }

    @Test
    void playersAreDeduplicatedIndependentlyAndCanBeRemoved() {
        GrapplingHookInputGate gate = new GrapplingHookInputGate(50L);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertTrue(gate.tryAcquire(first, 2_000L));
        assertTrue(gate.tryAcquire(second, 2_000L));
        gate.remove(first);
        assertTrue(gate.tryAcquire(first, 2_001L));
    }
}
