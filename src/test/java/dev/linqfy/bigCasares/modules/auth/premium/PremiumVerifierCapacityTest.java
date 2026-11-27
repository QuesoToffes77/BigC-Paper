package dev.linqfy.bigCasares.modules.auth.premium;

import org.junit.jupiter.api.Test;
import java.util.concurrent.Semaphore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PremiumVerifierCapacityTest {
    @Test
    void deniedRateCheckReleasesConnectionCapacity() {
        Semaphore permits = new Semaphore(1);
        assertFalse(PremiumVerifierServer.acquireForAllowedConnection(permits, () -> false));
        assertEquals(1, permits.availablePermits());
        assertTrue(PremiumVerifierServer.acquireForAllowedConnection(permits, () -> true));
        assertEquals(0, permits.availablePermits());
        permits.release();
    }

    @Test
    void failedRateCheckAlsoReleasesCapacity() {
        Semaphore permits = new Semaphore(1);
        assertThrows(IllegalStateException.class, () -> PremiumVerifierServer.acquireForAllowedConnection(permits,
            () -> { throw new IllegalStateException("failure"); }));
        assertEquals(1, permits.availablePermits());
    }
}
