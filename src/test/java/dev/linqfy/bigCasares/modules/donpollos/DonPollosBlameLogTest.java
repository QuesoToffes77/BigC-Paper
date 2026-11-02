package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosBlameLogTest {

    private final DonPollosBlameLog<String> log = new DonPollosBlameLog<>();
    private final UUID player = UUID.randomUUID();

    @Test
    void damageEventDuringSpecialKeepsTheSpecialCause() {
        log.special(player, "LASER", 10, () -> {
            assertTrue(log.specialInProgress());
            // el player.damage(x, ravager) dispara el evento de melee del boss
            assertFalse(log.ram(player, "RAM", 10));
        });
        assertFalse(log.specialInProgress());
        assertEquals("LASER", log.take(player, 11, 100));
    }

    @Test
    void nonLethalSpecialThenRealRamIsRam() {
        log.special(player, "METEOR", 10, () -> log.ram(player, "RAM", 10));
        assertTrue(log.ram(player, "RAM", 30));
        assertEquals("RAM", log.take(player, 31, 100));
    }

    @Test
    void contextIsClearedEvenIfDamageThrows() {
        try {
            log.special(player, "BEAM", 1, () -> {
                throw new IllegalStateException();
            });
        } catch (IllegalStateException expected) {
            // ok
        }
        assertFalse(log.specialInProgress());
    }

    @Test
    void oldBlameExpires() {
        log.blame(player, "RAM", 0);
        assertNull(log.take(player, 101, 100));
        assertNull(log.take(player, 101, 100), "se olvida al sacarla");
    }
}
