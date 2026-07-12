package dev.linqfy.bigCasares.modules.geyser;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeyserSubscriptionLifecycleTest {

    @Test
    void unregistersSubscribersExactlyOnce() {
        AtomicInteger unregistrations = new AtomicInteger();
        GeyserSubscriptionLifecycle lifecycle = new GeyserSubscriptionLifecycle(unregistrations::incrementAndGet);

        assertTrue(lifecycle.close());
        assertFalse(lifecycle.close());

        assertEquals(1, unregistrations.get());
    }
}
