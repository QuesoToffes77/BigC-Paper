package dev.linqfy.bigCasares.modules.loot;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathLootGuardTest {

    @Test
    void oneDeathCanOnlyBeProcessedOnce() {
        DeathLootGuard guard = new DeathLootGuard(2);
        UUID death = UUID.randomUUID();

        assertTrue(guard.markIfNew(death));
        assertFalse(guard.markIfNew(death));
    }

    @Test
    void boundedGuardForgetsTheOldestEntry() {
        DeathLootGuard guard = new DeathLootGuard(2);
        UUID first = UUID.randomUUID();
        guard.markIfNew(first);
        guard.markIfNew(UUID.randomUUID());
        guard.markIfNew(UUID.randomUUID());

        assertTrue(guard.markIfNew(first));
    }
}
