package dev.linqfy.bigCasares.modules.glider;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GliderSessionStoreTest {

    @Test
    void removingGliderAndLandingStopSession() {
        assertStops(GliderStopReason.ITEM_REMOVED);
        assertStops(GliderStopReason.LANDED);
    }

    @Test
    void lifecycleEventsCleanSessions() {
        assertStops(GliderStopReason.QUIT);
        assertStops(GliderStopReason.DEATH);
        assertStops(GliderStopReason.TELEPORT);
        assertStops(GliderStopReason.WORLD_CHANGE);
    }

    private static void assertStops(GliderStopReason reason) {
        GliderSessionStore store = new GliderSessionStore();
        UUID player = UUID.randomUUID();
        store.start(player, GliderTier.III, 20L, 100.0);
        assertTrue(store.isActive(player));
        store.stop(player, reason);
        assertFalse(store.isActive(player));
    }
}
