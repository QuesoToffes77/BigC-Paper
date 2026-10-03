package dev.linqfy.bigCasares.modules.grapplinghook;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrappleCooldownServiceTest {

    private final UUID player = UUID.randomUUID();

    @Test
    void startsOffCooldown() {
        GrappleCooldownService service = new GrappleCooldownService();

        assertFalse(service.isOnCooldown(player, 1_000L));
        assertEquals(0L, service.remainingMillis(player, 1_000L));
    }

    @Test
    void cooldownBlocksAndExpires() {
        GrappleCooldownService service = new GrappleCooldownService();
        service.start(player, 5_000L, 10_000L);

        assertTrue(service.isOnCooldown(player, 12_000L));
        assertEquals(3_000L, service.remainingMillis(player, 12_000L));
        assertFalse(service.isOnCooldown(player, 15_001L));
        assertEquals(0L, service.remainingMillis(player, 15_001L));
    }

    @Test
    void playersAreIsolated() {
        GrappleCooldownService service = new GrappleCooldownService();
        UUID other = UUID.randomUUID();
        service.start(player, 5_000L, 10_000L);

        assertTrue(service.isOnCooldown(player, 12_000L));
        assertFalse(service.isOnCooldown(other, 12_000L));
    }

    @Test
    void restartExtendsTheCooldown() {
        GrappleCooldownService service = new GrappleCooldownService();
        service.start(player, 5_000L, 10_000L);
        service.start(player, 2_000L, 12_000L);

        assertTrue(service.isOnCooldown(player, 14_500L));
        assertFalse(service.isOnCooldown(player, 14_001L + 4_000L));
    }

    @Test
    void clearRemovesAllState() {
        GrappleCooldownService service = new GrappleCooldownService();
        service.start(player, 60_000L, 0L);
        service.start(UUID.randomUUID(), 60_000L, 0L);
        assertEquals(2, service.activeCount());

        service.clear();

        assertEquals(0, service.activeCount());
        assertFalse(service.isOnCooldown(player, 1_000L));
    }

    @Test
    void ignoresNonPositiveDurationsAndNullPlayers() {
        GrappleCooldownService service = new GrappleCooldownService();
        service.start(player, 0L, 0L);
        service.start(null, 5_000L, 0L);

        assertFalse(service.isOnCooldown(player, 1L));
        assertEquals(0, service.activeCount());
    }
}
